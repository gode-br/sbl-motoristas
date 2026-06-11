# Midnight Driver 🚗💨

**Midnight Driver** é uma robusta plataforma de gestão financeira offline-first, projetada especificamente para motoristas de aplicativos parceiros (Uber, 99, InDrive, etc.) maximizarem seus lucros cotidianos de faturamento e planejarem suas despesas operacionais com inteligência.

O aplicativo integra captura ativa de tela por acessibilidade, agendamentos remotos, notificações persistentes de turnos em andamento e sincronização em tempo real com o ecossistema Firebase (Firestore e Auth).

---

## 🚀 Principais Funcionalidades

### 1. Sistema Inteligente de Turnos (`RideShift`)
*   **Abertura e Fechamento de Turno**: O motorista abre o turno definindo o troco em mãos (dinheiro inicial) e o odômetro inicial. Ao longo da jornada, acompanha ganhos e custos operacionais ativos.
*   **Edição Rica de Turno**: Agora o motorista pode visualizar e gerenciar, em listagem direta, todos os ganhos e gastos do turno concluído, modificando seus valores de maneira precisa e independente ou zerando valores indesejados de forma intuitiva.
*   **Caixa e Lucro Líquido**: Calcula faturamentos brutos e lucros líquidos instantâneos, excluindo gastos operacionais cadastrados.

### 2. Automação Rastreável com Serviço de Acessibilidade (`ScreenMonitorAccessibilityService`)
*   Monitora em segundo plano os fluxos de corrida de aplicativos parceiros (como o **InDrive**). 
*   **Rastreio em Tempo Real**: Captura dinamicamente o valor da corrida exibido na tela no momento de sua aceitação e conclusão, registrando-a de forma nativa no banco de dados local.
*   **Cálculo Automatizado de Taxas**: Desconte automaticamente taxas predeterminadas das plataformas parceiras (por exemplo, a taxa padrão de 11.3% do InDrive é automaticamente computada como um custo operacional "Taxa inDrive" no turno).

### 3. Gerenciamento de Despesas Fixas (`FixedExpense`)
*   Permite planejar metas financeiras deduzindo contas recorrentes (Seguro, Aluguel, IPVA, MEI).
*   **Alarmes Inteligentes (`ExpenseAlarmReceiver`)**: Dispara lembretes de vencimento para garantir que o motorista nunca pague juros por atraso.
*   Controle visual de despesas quitadas diretamente pela interface.

### 4. Sincronização na Nuvem em Tempo Real (`FirebaseSyncManager`)
*   **Login Transparente**: Sistema integrado com Firebase Authentication, suportando cadastro e login por e-mail e integração nativa com o **Google Sign-In**.
*   **Backup e Restauração Sincronizados**: Dados locais armazenados no Room DB são sincronizados com o Cloud Firestore de forma incremental, garantindo que o motorista não perca seu histórico financeiro caso mude de dispositivo.

### 5. Notificações Persistentes de Atividade (`ExpenseNotificationHelper`)
*   **Atividade Contínua**: Quando um turno está em aberto, uma notificação contínua (`Ongoing`) é fixada no sistema operacional, mostrando os ganhos brutos, custos acumulados e o saldo em tempo real da jornada ativa.

---

## 🛠️ Detalhes de Arquitetura & Stack Tecnológica

O app segue os melhores padrões internacionais do ecossistema Android:

*   **Linguagem**: Kotlin (Modern Android Development).
*   **UI Engine**: Jetpack Compose com Material Design 3, utilizando uma interface dark sob medida para menor fadiga ocular durante a noite.
*   **Banco de Dados Local**: Room Database para persistência com suporte a consultas complexas e reatividade instantânea via Kotlin Flows.
*   **Arquitetura**: MVVM (Model-View-ViewModel) acoplada com Repository Pattern para separação clara de responsabilidades.
*   **Injeção de Dependências**: Constructor Injection de fácil manutenção, acoplado nos pontos de inicialização do ciclo de vida da `MainActivity`.
*   **Asincronia**: Kotlin Coroutines e SharedFlows/StateFlows para atualizações instantâneas de estado de forma thread-safe.

---

## 📁 Estrutura de Pastas e Componentes Principais

```text
/app/src/main/java/com/example/
│
├── MainActivity.kt                      # Orquestrador da navegação e injeção de dependências do App
│
├── data/                               # Camada de Persistência e Dados
│   ├── FinanceEntities.kt              # Definições de entidades de banco (RideShift, FixedExpense, FinanceCategory)
│   ├── FinanceDao.kt                   # Queries SQLite Room otimizadas
│   ├── FinanceDatabase.kt              # Instância do banco de dados Room local
│   ├── FinanceRepository.kt            # Abstração de acesso a dados (Local Room + Firebase Cloud API)
│   └── FirebaseSyncManager.kt          # Gerenciamento de AuthService e Firestore Sync Engine
│
└── ui/                                 # Camada de Interface do Usuário (Compose)
    ├── theme/                          # Temas, Tipografias, Cores M3 (Dark Theme Prioritário)
    ├── DashboardScreen.kt              # Tela inicial de Resumo, Metas e Desempenho Mensal
    ├── ShiftsScreen.kt                 # Gestão de Turnos (Ativo, Histórico, Diálogos de Edição e Criação)
    ├── PeriodSummariesScreen.kt        # Relatórios financeiros acumulados de períodos
    ├── FixedExpensesScreen.kt          # Controle de Contas Recorrentes e Alarmes
    ├── CategoriesScreen.kt             # Customização de fontes de receitas/despesas
    ├── TransparentLoginScreen.kt       # Interface minimalista de Login/Nuvem do Usuário
    ├── ExpenseNotificationHelper.kt    # Auxiliar de geração de notificações contínuas e agendadas
    └── ScreenMonitorAccessibilityService.kt # Capturador dinâmico de faturamento via acessibilidade de tela
```

---

## 🏁 Como Rodar / Desenvolver o Projeto

1.  Abra o projeto no **Android Studio**.
2.  Insira as credenciais e conexões do Firebase em seu arquivo correspondente no painel de segredos (Secrets Panel do AI Studio).
3.  **Configuração Concluída**: O arquivo `google-services.json` já está totalmente configurado e integrado no diretório `/app`, habilitando a sincronização em tempo real na nuvem do Firebase Firestore e Google Sign-In nativo legítimo.
4.  Execute a compilação utilizando as tarefas Gradle recomendadas.
5.  **Atenção**: Para testar a captura automática de corridas do InDrive, certifique-se de conceder a permissão de **Serviço de Acessibilidade** ao app **Midnight Driver** nas configurações de acessibilidade do aparelho / emulador.
