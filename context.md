# Contexto do Projeto: Galeria de Defensores Offline

## Visão Geral
App Android (Kotlin) para fichas de 3D&T (Alpha/Gaiden). Arquitetura MVVM, Hilt, Room, Kotlinx Serialization e build via Docker.

## Estado Atual
1. **Arquitetura**: Implementado Clean Architecture com Casos de Uso (ex: LoadCharacterUseCase), Injeção de Dependência via Hilt e reatividade via StateFlow.
2. **Estabilização**: Sistema de regras customizado está estável, sem crashes ao criar/editar recursos.
3. **Persistência**: Migrado para Room e Kotlinx Serialization; carregamento one-shot para evitar "Flow Echo".
4. **UI**: Adaptadores unificados e barras de progresso funcionais.

## Últimas Alterações Realizadas
- Fix (UI): Barra de progresso dos recursos corrigida usando `progressTintList` para manter a proporção visual e o fundo cinza.
- Fix (Logic): Alteração em 'Resistência' agora afeta apenas o valor máximo de PV/PM, mantendo o valor atual intacto.
- Refactor: Limpeza de arquivos legados (Firebase, backups antigos e logs).
- Test: Implementação de testes unitários com MockK para o `ResourcesAdapter`.

## Próximos Passos
- Monitorar estabilidade geral após a migração completa para Room.
- [Aguardando novas definições do usuário]

## Regras de Ouro (AI-Rules)
- **TDD Incremental**: Escrever teste -> Parar -> Aguardar OK -> Implementar.
- **IDs Únicos**: Sempre gerar novo UUID ao mover item para a ficha.
- **Reatividade**: Usar sempre `ruleSystemViewModel.ruleSystem.value` para garantir o estado mais recente do sistema de regras.
