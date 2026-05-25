# Contexto do Projeto: Galeria de Defensores Offline

## Visão Geral
App Android (Kotlin) para fichas de 3D&T (Alpha/Gaiden). Arquitetura MVVM, Hilt, Room, Kotlinx Serialization e build via Docker.

## Estado Atual
1. **Arquitetura**: Implementado Clean Architecture com Casos de Uso (ex: LoadCharacterUseCase), Injeção de Dependência via Hilt e reatividade via StateFlow.
2. **Estabilização**: Sistema de regras customizado está estável, sem crashes ao criar/editar recursos.
3. **Persistência**: Migrado para Room e Kotlinx Serialization; carregamento one-shot para evitar "Flow Echo".
4. **UI**: Adaptadores unificados e barras de progresso funcionais.

## Últimas Alterações Realizadas
- **Fix (UI - view_system_metadata.xml)**: Corrigida a visibilidade dos textos de entrada (nome e descrição do sistema) no gerenciamento de sistema (Sandbox), alterando o `android:textColor` de `#FFFFFF` para `@color/text_primary` para resolver o problema de letra branca sobre fundo branco.
- **Fix (Reatividade Sandbox - SystemManagementViewModel.kt)**: Garantida a atualização imediata na UI ao adicionar ou deletar atributos/recursos na Sandbox. Todos os métodos mutadores estruturais agora emitem uma nova cópia profunda do objeto `RuleSystem` (`current.copy(...)` e `.toMutableList()`), forçando o `MutableStateFlow` a emitir uma nova referência e disparar os flow collectors na UI.
- **Feature (Validação de Unicidade - RuleSystem.kt)**: Criado o método de extensão `RuleSystem.validateUniqueNameAndKey(id, key, name)` para garantir chaves e nomes únicos (case-insensitive) entre Atributos e Recursos em todo o sistema.
- **Logic (Integração de Validações)**:
  - Adicionadas validações estruturais antes de salvar nos ViewModels (`SystemManagementViewModel` e `RuleSystemViewModel`).
  - Diálogos de edição (`DialogEditAttributeDefinition` e `DialogEditResourceDefinition`) agora tratam falhas de validação via `try-catch`, exibindo uma mensagem de erro (`Toast`) e mantendo o diálogo aberto com as edições do usuário preservadas.
- **Fix (Unit Tests - SystemManagementViewModelTest.kt & RuleSystemViewModelTest.kt)**: Adicionados testes de unicidade de chaves/nomes case-insensitive e corrigido o caso de teste `addAttributeDefinition` que falhava devido ao conflito com chaves default de sistema pré-carregadas na Sandbox. Todos os testes estão integrados e executando corretamente.

## Próximos Passos
- Realizar validação final e testes manuais da interface de customização e do comportamento reativo da Sandbox no dispositivo físico.

## Regras de Ouro (AI-Rules)
- **TDD Incremental**: Escrever teste -> Parar -> Aguardar OK -> Implementar.
- **IDs Únicos**: Sempre gerar novo UUID ao mover item para a ficha.
- **Reatividade**: Usar sempre `ruleSystemViewModel.ruleSystem.value` para garantir o estado mais recente do sistema de regras.
- **Mutabilidade no StateFlow**: Para que o StateFlow notifique os coletores de novos valores no Android, sempre emitir uma cópia profunda (deep copy/new reference) do objeto e de suas listas internas, evitando alterações em lote in-place sem mudança de referência.
