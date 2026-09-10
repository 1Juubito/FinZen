# Error404Saldo — Controle Financeiro Premium

Aplicativo Android de controle financeiro pessoal inspirado no **Mobills**, com
design premium em Material 3, modo claro/escuro e foco total em privacidade
(100% offline — seus dados ficam apenas no aparelho).

> Feito com Kotlin + Jetpack Compose. Abra no Android Studio e rode.

---

## ✨ Funcionalidades

- **Visão geral (Dashboard):** saldo total com cartão em gradiente, ocultar/mostrar
  saldo, resumo de receitas e despesas do mês, gráfico de rosca de despesas por
  categoria, contas em destaque e transações recentes.
- **Transações:** receitas, despesas e transferências entre contas; lista agrupada
  por dia, navegação por mês, filtros por tipo e resumo do período.
- **Lançamento rápido:** teclado numérico próprio, seletor de tipo, categoria,
  conta/cartão, data, status pago/pendente, observações e **parcelamento** no
  cartão de crédito.
- **Contas:** carteira, conta corrente, poupança e investimentos com saldo
  calculado automaticamente.
- **Cartões de crédito:** cartões com visual realista, **fatura atual**, limite
  disponível, uso do limite e detalhe da fatura por mês.
- **Categorias:** receitas e despesas personalizáveis com seletor de ícone e cor.
- **Planejamento (orçamentos):** limite de gastos por categoria com barra de
  progresso e alerta de estouro.
- **Relatórios:** rosca de despesas/receitas por categoria e gráfico de barras
  Receitas × Despesas dos últimos 6 meses.
- **Metas:** objetivos de economia com progresso, prazo e aportes/retiradas.
- **Configurações:** tema (Sistema/Claro/Escuro), carregar dados de exemplo e
  apagar transações.

## 🎨 Design

- Material 3 (Material You) com paleta de marca **esmeralda** própria.
- Tema claro e escuro com cores semânticas de finanças (receita/despesa/transferência).
- Tipografia, formas arredondadas e cartões com profundidade para um visual premium.
- Ícone do app adaptativo (vetorial) com versão monocromática para o tema do sistema.
- Gráficos (rosca e barras) desenhados sob medida com Compose Canvas.

## 🧱 Arquitetura & Stack

| Camada | Tecnologia |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Apresentação | MVVM (`ViewModel` + `StateFlow`) |
| Dados | Room (offline-first), DataStore (preferências) |
| Async | Kotlin Coroutines + Flow |
| Injeção de dependências | Container manual (`AppContainer`) + `viewModelFactory` |

Estrutura de pacotes (`com.finzen.app`):

```
data/
  local/        Room: entidades, DAOs, conversores, database
  model/        enums e modelos derivados
  repository/   repositórios (regras de acesso a dados)
  seed/         categorias/contas padrão e dados de exemplo
di/             AppContainer + AppViewModelProvider (fábrica de ViewModels)
ui/
  theme/        cores, tipografia, formas, tema
  components/   componentes reutilizáveis (avatares, gráficos, listas)
  icons/        catálogo de ícones e paleta de cores
  navigation/   rotas, NavHost e scaffold com barra inferior + FAB
  dashboard/ transactions/ transaction/ accounts/ cards/
  categories/ budgets/ reports/ goals/ settings/ more/
```

## 🚀 Como rodar

Pré-requisitos: **Android Studio** (Ladybug ou mais recente) e um dispositivo/emulador
com **Android 8.0 (API 26)** ou superior.

1. Abra a pasta do projeto no Android Studio (`File → Open`).
2. Aguarde o Gradle sincronizar (baixa as dependências automaticamente).
3. Selecione um emulador/dispositivo e clique em **Run ▶**.

Pela linha de comando (com o Android SDK instalado e `local.properties` apontando
para o SDK):

```bash
./gradlew assembleDebug      # gera o APK de debug
./gradlew installDebug       # instala em um dispositivo conectado
```

> Dica: na primeira execução o app cria categorias e contas padrão. Em
> **Mais → Configurações → Dados**, use “Carregar dados de exemplo” para ver os
> gráficos e relatórios preenchidos.

## ⚙️ Detalhes técnicos

- `minSdk 26`, `targetSdk 35`, `compileSdk 35`.
- Kotlin 2.0 com o plugin de compilação do Compose; Room via KSP.
- Sem permissão de internet — todos os dados ficam locais (Room/DataStore).

---

Projeto criado como demonstração de um app financeiro completo e elegante.
