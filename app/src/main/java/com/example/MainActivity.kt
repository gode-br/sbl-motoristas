package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FinanceDatabase
import com.example.data.FinanceRepository
import com.example.data.FirebaseSyncManager
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.R

class MainActivity : ComponentActivity() {
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    // Permissão resolvida
  }

  override fun onResume() {
    super.onResume()
    val prefs = getSharedPreferences("expense_notif_prefs", MODE_PRIVATE)
    prefs.edit().putLong("last_app_access_time", System.currentTimeMillis()).apply()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Inicializa canal de notificações e agenda verificações periódicas
    ExpenseNotificationHelper.createNotificationChannel(this)
    ExpenseNotificationHelper.schedulePeriodicCheck(this)

    // Solicita permissão de notificação dinamicamente no Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    setContent {
      MyApplicationTheme {
        val context = LocalContext.current
        val database = remember { FinanceDatabase.getDatabase(context) }
        val repository = remember { FinanceRepository(database.financeDao()) }
        val viewModel: FinanceViewModel = viewModel(factory = FinanceViewModelFactory(repository))

        val summary by viewModel.summaryState.collectAsStateWithLifecycle()
        val shifts by viewModel.allShifts.collectAsStateWithLifecycle()
        val fixedExpenses by viewModel.allFixedExpenses.collectAsStateWithLifecycle()
        val categories by viewModel.allCategories.collectAsStateWithLifecycle()

        var currentTab by remember { mutableStateOf(0) }
        var showCloudSyncSheet by remember { mutableStateOf(false) }

        val coroutineScope = rememberCoroutineScope()
        val loginPrefs = remember { context.getSharedPreferences("midnight_login_prefs", android.content.Context.MODE_PRIVATE) }
        var firstLoginCompleted by remember { mutableStateOf(loginPrefs.getBoolean("first_login_completed", false)) }
        val firebaseUser by FirebaseSyncManager.currentUser.collectAsState()

        val isAppUnlocked = firstLoginCompleted || firebaseUser != null

        // Launcher do Google Sign-In para a tela de login
        val googleSignInLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
          val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
          try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
              coroutineScope.launch {
                val user = FirebaseSyncManager.signInWithGoogle(idToken)
                if (user != null) {
                  loginPrefs.edit().putBoolean("first_login_completed", true).apply()
                  firstLoginCompleted = true
                  FirebaseSyncManager.syncOnLogin(repository, shifts, fixedExpenses, categories)
                }
              }
            }
          } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro login Google", e)
          }
        }

        // Se sair do Firebase, removemos o bypass pra exigir login novamente
        LaunchedEffect(firebaseUser) {
          if (firebaseUser == null) {
            loginPrefs.edit().putBoolean("first_login_completed", false).apply()
            firstLoginCompleted = false
          }
        }

        LaunchedEffect(shifts) {
          val activeShift = shifts.find { it.isOpen }
          ExpenseNotificationHelper.updateActiveShiftNotification(context, activeShift)
        }

        Box(modifier = Modifier.fillMaxSize()) {
          Scaffold(
            modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
            topBar = {
            TopAppBar(
              title = {
                Text(
                  text = when (currentTab) {
                    0 -> "Resumo"
                    1 -> "Turnos"
                    2 -> "Análise por Período"
                    3 -> "Custos Mensais Fixos"
                    else -> "Tipos de Lançamento"
                  },
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                  modifier = Modifier.testTag("app_top_bar_title")
                )
              },
              actions = {
                val user by FirebaseSyncManager.currentUser.collectAsState()
                IconButton(
                  onClick = { showCloudSyncSheet = true },
                  modifier = Modifier.testTag("top_bar_cloud_sync_button")
                ) {
                  Icon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = "Configurar Backup na Nuvem",
                    tint = if (user != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                  )
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
              )
            )
          },
          bottomBar = {
            NavigationBar(
              modifier = Modifier.testTag("app_bottom_nav"),
              containerColor = MaterialTheme.colorScheme.surface
            ) {
              // Aba 1: Resumo
              NavigationBarItem(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                icon = {
                  Icon(
                    imageVector = if (currentTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "Menu Resumo"
                  )
                },
                label = { Text("Resumo") },
                modifier = Modifier.testTag("nav_tab_summary")
              )
              // Aba 2: Turnos
              NavigationBarItem(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                icon = {
                  Icon(
                    imageVector = if (currentTab == 1) Icons.Filled.DirectionsCar else Icons.Outlined.DirectionsCar,
                    contentDescription = "Menu Turnos"
                  )
                },
                label = { Text("Turnos") },
                modifier = Modifier.testTag("nav_tab_shifts")
              )
              // Aba 3: Relatórios Períodos
              NavigationBarItem(
                selected = currentTab == 2,
                onClick = { currentTab = 2 },
                icon = {
                  Icon(
                    imageVector = if (currentTab == 2) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                    contentDescription = "Menu Períodos"
                  )
                },
                label = { Text("Períodos") },
                modifier = Modifier.testTag("nav_tab_periods")
              )
              // Aba 4: Fixos
              NavigationBarItem(
                selected = currentTab == 3,
                onClick = { currentTab = 3 },
                icon = {
                  Icon(
                    imageVector = if (currentTab == 3) Icons.Filled.AccountBalance else Icons.Outlined.AccountBalance,
                    contentDescription = "Menu Fixos"
                  )
                },
                label = { Text("Fixos") },
                modifier = Modifier.testTag("nav_tab_fixed")
              )
              // Aba 5: Filtros/Categorias Customizadas
              NavigationBarItem(
                selected = currentTab == 4,
                onClick = { currentTab = 4 },
                icon = {
                  Icon(
                    imageVector = if (currentTab == 4) Icons.Filled.Tune else Icons.Outlined.Tune,
                    contentDescription = "Menu Config Tipos"
                  )
                },
                label = { Text("Tipos") },
                modifier = Modifier.testTag("nav_tab_categories")
              )
            }
          }
        ) { innerPadding ->
          when (currentTab) {
            0 -> DashboardScreen(
              summary = summary,
              modifier = Modifier.padding(innerPadding)
            )
            1 -> ShiftsScreen(
              shifts = shifts,
              categories = categories,
              onAddShift = { earnings, costs, km, hours, notes, date ->
                viewModel.addShift(earnings, costs, km, hours, notes, date)
              },
              onDeleteShift = { id -> viewModel.deleteShift(id) },
              onStartActiveShift = { cash, accounts, startOdm, date ->
                viewModel.startNewActiveShift(cash, accounts, startOdm, date)
              },
              onAddEarningToActiveShift = { id, cat, amount ->
                viewModel.addEarningToShift(id, cat, amount)
              },
              onAddCostToActiveShift = { id, cat, amount ->
                viewModel.addCostToShift(id, cat, amount)
              },
              onUpdateShiftDetails = { id, km, hours, notes, date ->
                viewModel.updateShiftDetails(id, km, hours, notes, date)
              },
              onCloseShift = { id, km, hours, notes, endOdm, endCash ->
                viewModel.closeShift(id, km, hours, notes, endOdm, endCash)
              },
              onRemoveEarningFromActiveShift = { id, cat ->
                viewModel.removeEarningFromShift(id, cat)
              },
              onEditEarningInActiveShift = { id, cat, amount ->
                viewModel.editEarningInShift(id, cat, amount)
              },
              onRemoveCostFromActiveShift = { id, cat ->
                viewModel.removeCostFromShift(id, cat)
              },
              onEditCostInActiveShift = { id, cat, amount ->
                viewModel.editCostInShift(id, cat, amount)
              },
              onRemoveInDriveRideFromActiveShift = { id, ts ->
                viewModel.removeInDriveRideFromShift(id, ts)
              },
              onEditInDriveRideInActiveShift = { id, ts, value, cancelled ->
                viewModel.editInDriveRideInShift(id, ts, value, cancelled)
              },
              onEditStartingCashInShift = { id, amount ->
                viewModel.editStartingCashInShift(id, amount)
              },
              onEditEndCashInShift = { id, amount ->
                viewModel.editEndCashInShift(id, amount)
              },
              modifier = Modifier.padding(innerPadding)
            )
            2 -> PeriodSummariesScreen(
              shifts = shifts,
              fixedExpenses = fixedExpenses,
              modifier = Modifier.padding(innerPadding)
            )
            3 -> FixedExpensesScreen(
              fixedExpenses = fixedExpenses,
              onAddExpense = { name, amount, dueDay, isPaid ->
                viewModel.addFixedExpense(name, amount, dueDay, isPaid)
              },
              onToggleExpensePaid = { expense ->
                viewModel.toggleFixedExpensePaid(expense)
              },
              onDeleteExpense = { id -> viewModel.deleteFixedExpense(id) },
              modifier = Modifier.padding(innerPadding)
            )
            4 -> CategoriesScreen(
              categories = categories,
              onAddCategory = { name, type ->
                viewModel.addCategory(name, type)
              },
              onEditCategory = { category ->
                viewModel.updateCategory(category)
              },
              onDeleteCategory = { id ->
                viewModel.deleteCategory(id)
              },
              modifier = Modifier.padding(innerPadding)
            )
          }
        }

        if (showCloudSyncSheet) {
          CloudSyncBottomSheet(
            onDismissRequest = { showCloudSyncSheet = false },
            shifts = shifts,
            fixedExpenses = fixedExpenses,
            categories = categories,
            repository = repository
          )
        }

        if (!isAppUnlocked) {
          TransparentLoginScreen(
            onLoginSuccess = {
              firstLoginCompleted = true
            },
            onGoogleSignInClick = {
              val client = FirebaseSyncManager.getGoogleSignInClient(context)
              googleSignInLauncher.launch(client.signInIntent)
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}
}
