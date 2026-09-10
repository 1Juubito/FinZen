# 💚 FinZen — Controle Financeiro Pessoal

> Aplicativo Android nativo de gestão financeira pessoal, construído com **Kotlin + Jetpack Compose**, focado em arquitetura limpa, UX premium e funcionalidades que vão além do CRUD básico.

---

## 🚀 Funcionalidades

### 💳 Gestão de Transações
- Cadastro de **receitas, despesas e transferências** com categorias personalizadas.
- Edição e exclusão de qualquer lançamento de forma ágil.
- **Transações recorrentes** — cadastre uma vez, o app registra automaticamente.
- Atalhos nativos do Android (*App Shortcuts*) para lançar despesa, receita ou transferência direto da home.

### 📊 Relatórios e Análise
- Tela de **relatórios mensais** com visão detalhada de entradas e saídas.
- **Regras de categoria** — defina critérios automáticos para classificar lançamentos.
- Controle de **saldo disponível** (*Spendable*) para orçamento inteligente.

### 🔐 Segurança
- **Autenticação biométrica** na abertura do app via `BiometricGate`.
- Dados armazenados **100% localmente** com Room — nenhum dado sai do dispositivo.

### 🖼️ Widget de Saldo
- Widget nativo para a tela inicial do Android exibindo o saldo em tempo real.
- Design em pílula (*pill*) minimalista, atualizando automaticamente.

### 🔔 Notificações
- Sistema de notificações para lembretes de lançamentos agendados.

---

## 🏗️ Arquitetura

```
com.finzen.app/
├── data/          # Repositórios, DAOs, entidades Room
├── di/            # Injeção de dependência (Hilt)
├── notifications/ # Canais e workers de notificação
├── shortcuts/     # App Shortcuts do Android
├── ui/            # Telas e ViewModels (Jetpack Compose)
│   ├── reports/
│   ├── recurring/
│   ├── rules/
│   ├── security/
│   ├── settings/
│   ├── spendable/
│   ├── theme/
│   ├── transaction/
│   └── widget/
└── util/          # DateUtils, Money, MonthRef
```

O projeto segue **MVVM + Clean Architecture**: cada tela tem seu `Screen.kt` (Compose UI) e seu `ViewModel.kt` (lógica e estado), comunicando-se com os repositórios da camada `data/`.

---

## 🛠️ Tecnologias

| Camada           | Stack                                              |
|------------------|----------------------------------------------------|
| Linguagem        | Kotlin                                             |
| UI               | Jetpack Compose · Material 3                       |
| Arquitetura      | MVVM · Clean Architecture                          |
| Banco de dados   | Room (SQLite local)                                |
| DI               | Hilt                                               |
| Segurança        | AndroidX Biometric                                 |
| Widget           | Android App Widget API                             |
| Atalhos          | Android App Shortcuts API                          |
| Notificações     | Android Notification API · WorkManager             |

---

## 📁 Estrutura do Projeto

```text
finzen/
├── app/
│   ├── src/main/
│   │   ├── java/com/finzen/app/    # Código-fonte Kotlin
│   │   └── res/                    # Layouts, drawables, valores
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── libs.versions.toml              # Version catalog
└── README.md
```

---

## ▶️ Como Rodar

### Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 17+
- Android SDK (API 26+)

### Passos

1. Clone o repositório:
```bash
git clone https://github.com/1Juubito/finzen.git
```

2. Abra no Android Studio: **File → Open → pasta do projeto**

3. Aguarde o Gradle sincronizar as dependências.

4. Execute no dispositivo ou emulador:
```
Run → Run 'app'   (ou Shift+F10)
```

> 💡 O app usa banco de dados **100% local** — nenhuma configuração de backend ou chave de API é necessária.

---

## 📸 Destaques de UI/UX

- **Design System próprio** — paleta de cores `FinanceColors`, tipografia `Neo` e shapes customizados.
- **Dark Mode** nativo com tema coerente em todas as telas.
- **Widget minimalista** na home screen com saldo atualizado em tempo real.
- **Atalhos rápidos** no ícone do app para os três tipos de lançamento.
- **Biometria** integrada como camada de privacidade antes de exibir dados financeiros.

---

## 👨‍💻 Autor

**Allan Crisanto**
Técnico de TI · Graduado em ADS (Uninter) · Pós-graduando em Cibersegurança Ofensiva — Red Team Operations (FIAP/PosTech)
