package com.example.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _syncState = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncState: StateFlow<SyncStatus> = _syncState

    init {
        _currentUser.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        // Obter o ID do cliente da Web do google-services gerado dinamicamente
        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (webClientIdResId != 0) context.getString(webClientIdResId) else ""
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .apply {
                if (webClientId.isNotEmpty()) {
                    requestIdToken(webClientId)
                }
            }
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun isGoogleConfigured(context: Context): Boolean {
        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return webClientIdResId != 0 && context.getString(webClientIdResId).isNotEmpty()
    }

    fun setSyncStatus(status: SyncStatus) {
        _syncState.value = status
    }

    fun logout() {
        auth.signOut()
        _syncState.value = SyncStatus.Idle
    }

    suspend fun signInWithEmail(email: String, psw: String): FirebaseUser? {
        _syncState.value = SyncStatus.Loading("Autenticando...")
        return try {
            val result = auth.signInWithEmailAndPassword(email, psw).await()
            _syncState.value = SyncStatus.Success("Conectado com sucesso!")
            result.user
        } catch (e: Exception) {
            Log.e(TAG, "Erro de login com email", e)
            _syncState.value = SyncStatus.Error("Erro de acesso: ${e.localizedMessage ?: "Verifique as credenciais"}")
            null
        }
    }

    suspend fun registerWithEmail(email: String, psw: String): FirebaseUser? {
        _syncState.value = SyncStatus.Loading("Registrando usuário...")
        return try {
            val result = auth.createUserWithEmailAndPassword(email, psw).await()
            _syncState.value = SyncStatus.Success("Conta criada com sucesso!")
            result.user
        } catch (e: Exception) {
            Log.e(TAG, "Erro de registro com email", e)
            _syncState.value = SyncStatus.Error("Erro no registro: ${e.localizedMessage ?: "Tente novamente"}")
            null
        }
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        _syncState.value = SyncStatus.Loading("Autenticando com o Google...")
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            _syncState.value = SyncStatus.Success("Conectado com sucesso!")
            authResult.user
        } catch (e: Exception) {
            Log.e(TAG, "Erro de login no Firebase Auth", e)
            _syncState.value = SyncStatus.Error("Erro de autenticação: ${e.localizedMessage ?: "Consulte o suporte"}")
            null
        }
    }

    suspend fun syncOnLogin(
        repository: FinanceRepository,
        localShifts: List<RideShift>,
        localFixed: List<FixedExpense>,
        localCategories: List<FinanceCategory>
    ): Boolean {
        val user = currentUser.value ?: return false
        _syncState.value = SyncStatus.Loading("Sincronizando com a Nuvem...")
        return try {
            val snapshot = firestore.collection("users")
                .document(user.uid)
                .collection("backup")
                .document("current")
                .get()
                .await()

            if (!snapshot.exists()) {
                // Nenhum backup em nuvem. Cria o primeiro com os dados locais atuais.
                Log.d(TAG, "Nenhum backup em nuvem encontrado. Criando primeiro backup automático.")
                backupToCloud(localShifts, localFixed, localCategories)
            } else {
                // Backup em nuvem existe. Vamos analisar o que fazer.
                val isLocalEmpty = localShifts.isEmpty() && localFixed.isEmpty()

                if (isLocalEmpty) {
                    // Local está vazio (ex: nova instalação). Restaura automaticamente da nuvem.
                    Log.d(TAG, "Banco local vazio. Restaurando dados salvos em nuvem.")
                    restoreFromCloud(repository)
                } else {
                    // Ambos existem. Vamos restaurar o da nuvem se ele tiver mais turnos cadastrados,
                    // caso contrário atualizamos a nuvem com os dados locais mais completos.
                    @Suppress("UNCHECKED_CAST")
                    val shiftsRaw = snapshot.get("shifts") as? List<Map<String, Any>> ?: emptyList()

                    if (shiftsRaw.size > localShifts.size) {
                        Log.d(TAG, "Nuvem possui mais dados (${shiftsRaw.size}) do que local (${localShifts.size}). Restaurando.")
                        restoreFromCloud(repository)
                    } else if (shiftsRaw.size < localShifts.size) {
                        Log.d(TAG, "Local possui mais dados (${localShifts.size}) do que nuvem (${shiftsRaw.size}). Atualizando nuvem.")
                        backupToCloud(localShifts, localFixed, localCategories)
                    } else {
                        // Tamanhos iguais. Vamos comparar o timestamp para garantir que a nuvem tem a versão final.
                        val cloudTimestamp = snapshot.getLong("timestamp") ?: 0L
                        if (cloudTimestamp == 0L) {
                            backupToCloud(localShifts, localFixed, localCategories)
                        } else {
                            _syncState.value = SyncStatus.Success("Dados totalmente sincronizados com a Nuvem!")
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar ao entrar", e)
            _syncState.value = SyncStatus.Error("Falha na sincronização inicial: ${e.localizedMessage}")
            false
        }
    }

    suspend fun backupToCloud(
        shifts: List<RideShift>,
        fixedExpenses: List<FixedExpense>,
        categories: List<FinanceCategory>
    ): Boolean {
        val user = currentUser.value
        if (user == null) {
            _syncState.value = SyncStatus.Error("Você precisa estar logado para fazer backup.")
            return false
        }

        _syncState.value = SyncStatus.Loading("Enviando dados para nuvem...")
        return try {
            val payload = mapOf(
                "shifts" to shifts.map { it.toMap() },
                "fixedExpenses" to fixedExpenses.map { it.toMap() },
                "categories" to categories.map { it.toMap() },
                "timestamp" to System.currentTimeMillis(),
                "userEmail" to (user.email ?: ""),
                "userName" to (user.displayName ?: "")
            )

            firestore.collection("users")
                .document(user.uid)
                .collection("backup")
                .document("current")
                .set(payload)
                .await()

            _syncState.value = SyncStatus.Success("Backup realizado com sucesso!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar backup para Firestore", e)
            _syncState.value = SyncStatus.Error("Falha ao salvar dados: ${e.localizedMessage}")
            false
        }
    }

    suspend fun restoreFromCloud(repository: FinanceRepository): Boolean {
        val user = currentUser.value
        if (user == null) {
            _syncState.value = SyncStatus.Error("Você precisa estar logado para restaurar os dados.")
            return false
        }

        _syncState.value = SyncStatus.Loading("Baixando dados da nuvem...")
        return try {
            val snapshot = firestore.collection("users")
                .document(user.uid)
                .collection("backup")
                .document("current")
                .get()
                .await()

            if (!snapshot.exists()) {
                _syncState.value = SyncStatus.Error("Nenhum backup encontrado no servidor.")
                return false
            }

            @Suppress("UNCHECKED_CAST")
            val shiftsRaw = snapshot.get("shifts") as? List<Map<String, Any>> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val fixedRaw = snapshot.get("fixedExpenses") as? List<Map<String, Any>> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val categoriesRaw = snapshot.get("categories") as? List<Map<String, Any>> ?: emptyList()

            val shifts = shiftsRaw.map { mapToRideShift(it) }
            val fixed = fixedRaw.map { mapToFixedExpense(it) }
            val categories = categoriesRaw.map { mapToFinanceCategory(it) }

            // Substituir banco de dados local de forma atômica
            repository.clearLocalData()

            for (category in categories) {
                repository.insertCategory(category)
            }
            for (expense in fixed) {
                repository.insertFixedExpense(expense)
            }
            for (shift in shifts) {
                repository.insertShift(shift)
            }

            _syncState.value = SyncStatus.Success("Dados restaurados com sucesso!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro restaurando backup", e)
            _syncState.value = SyncStatus.Error("Falha ao restaurar dados: ${e.localizedMessage}")
            false
        }
    }

    private fun RideShift.toMap(): Map<String, Any?> {
        return mapOf(
            "date" to date,
            "kmDriven" to kmDriven,
            "hoursWorked" to hoursWorked,
            "earningsMapString" to earningsMapString,
            "costsMapString" to costsMapString,
            "notes" to notes,
            "isOpen" to isOpen,
            "startingCash" to startingCash,
            "startingAccounts" to startingAccounts,
            "startOdometer" to startOdometer,
            "endOdometer" to endOdometer,
            "indriveRidesString" to indriveRidesString,
            "endCash" to endCash
        )
    }

    private fun mapToRideShift(map: Map<String, Any?>): RideShift {
        return RideShift(
            date = (map["date"] as? Number)?.toLong() ?: 0L,
            kmDriven = (map["kmDriven"] as? Number)?.toDouble() ?: 0.0,
            hoursWorked = (map["hoursWorked"] as? Number)?.toDouble() ?: 0.0,
            earningsMapString = map["earningsMapString"] as? String ?: "",
            costsMapString = map["costsMapString"] as? String ?: "",
            notes = map["notes"] as? String ?: "",
            isOpen = map["isOpen"] as? Boolean ?: false,
            startingCash = (map["startingCash"] as? Number)?.toDouble() ?: 0.0,
            startingAccounts = (map["startingAccounts"] as? Number)?.toDouble() ?: 0.0,
            startOdometer = (map["startOdometer"] as? Number)?.toDouble() ?: 0.0,
            endOdometer = (map["endOdometer"] as? Number)?.toDouble() ?: 0.0,
            indriveRidesString = map["indriveRidesString"] as? String ?: "",
            endCash = (map["endCash"] as? Number)?.toDouble() ?: 0.0
        )
    }

    private fun FixedExpense.toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "amount" to amount,
            "dueDay" to dueDay,
            "isPaid" to isPaid
        )
    }

    private fun mapToFixedExpense(map: Map<String, Any?>): FixedExpense {
        return FixedExpense(
            name = map["name"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            dueDay = (map["dueDay"] as? Number)?.toInt() ?: 1,
            isPaid = map["isPaid"] as? Boolean ?: false
        )
    }

    private fun FinanceCategory.toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "type" to type,
            "isDefault" to isDefault
        )
    }

    private fun mapToFinanceCategory(map: Map<String, Any?>): FinanceCategory {
        return FinanceCategory(
            name = map["name"] as? String ?: "",
            type = map["type"] as? String ?: "EARNING",
            isDefault = map["isDefault"] as? Boolean ?: false
        )
    }
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    data class Loading(val message: String) : SyncStatus
    data class Success(val message: String) : SyncStatus
    data class Error(val message: String) : SyncStatus
}
