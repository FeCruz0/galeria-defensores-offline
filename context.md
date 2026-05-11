# Contexto do Projeto: Galeria de Defensores Offline

## Visão Geral
App Android (Kotlin) para fichas de 3D&T (Alpha/Gaiden). Arquitetura MVVM, Hilt, Room, Kotlinx Serialization e build via Docker.

## Estado Atual
1. **Estabilização**: Sistema de regras customizado está estável, sem crashes ao criar/editar recursos.
2. **Persistência**: Migrado para Kotlinx Serialization; reatividade via StateFlow com carregamento one-shot (LoadCharacterUseCase) para evitar "Flow Echo".
3. **UI**: Adaptadores de recursos e atributos unificados no `CharacterSheetFragment`.

## Últimas Alterações Realizadas
- Fix na exclusão de recursos customizados (confirmação adicionada e sincronização de dados corrigida).
- Tentativa de correção de "flicker" nas barras de progresso (desativado `supportsChangeAnimations` no RecyclerView e usado `setProgress(v, false)` no `ResourcesAdapter`).

## Pendência Crítica (Próximo Passo)
- **Barra de Progresso (Recursos)**: Apesar da remoção do flicker, as barras (PV, PM, etc.) não estão atualizando visualmente de forma proporcional aos pontos (comportamento de "barra de jogo"). O texto numérico atualiza, mas a barra visual (`ProgressBar`) não reflete a mudança de forma correta.

## Regras de Ouro (AI-Rules)
- **TDD Incremental**: Escrever teste -> Parar -> Aguardar OK -> Implementar.
- **IDs Únicos**: Sempre gerar novo UUID ao mover item para a ficha.
- **Reatividade**: Usar sempre `ruleSystemViewModel.ruleSystem.value` para garantir o estado mais recente do sistema de regras.
