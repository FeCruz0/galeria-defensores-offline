package com.galeria.defensores.data

import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.models.ModifierOption

object AdvantagesData {
    val defaultAdvantages = listOf(
        AdvantageItem(name = "Aceleração", cost = "1 ponto", description = "Permite múltiplos movimentos por turno e bônus de velocidade."),
        AdvantageItem(name = "Adaptador", cost = "1 ponto", description = "Permite alterar o tipo de dano do ataque (Físico ou Energia) sem penalidade."),
        AdvantageItem(name = "Aliado", cost = "1 ponto (cada)", description = "Um companheiro de Nível de 1 a 5."),
        AdvantageItem(name = "Alquimista", cost = "1 ponto", description = "Permite criar itens, reduzindo o custo em PM de rituais pela metade."),
        AdvantageItem(name = "Aparência Inofensiva", cost = "1 ponto", description = "Surpreende o oponente no 1º turno; bônus de +1 em testes de H para Manipulação."),
        AdvantageItem(name = "Arcano", cost = "4 pontos", description = "Acesso total às Magias Branca, Elemental e Negra, reduzindo 1 PM no custo."),
        AdvantageItem(name = "Área de Batalha", cost = "2 pontos", description = "Transporta o combate para uma dimensão. Efeitos à escolha: H+1 e A+2, ou F/PdF+2 e A+2, ou Ataques Especiais sem custo em PMs."),
        AdvantageItem(
            name = "Arena", 
            cost = "1 ponto", 
            description = "Recebe H+2 em um tipo de terreno específico. Escolha o seu terreno. Para comprar Arenas extras, você precisa recomprar a vantagem.",
            isModular = true,
            baseCostPt = 1,
            modifiers = listOf(
                ModifierOption("are_agua",          "Água",         0, "Praias, barcos, chuva, superfícies aquáticas ou submerso"),
                ModifierOption("are_ceu",           "Céu",          0, "Combate aéreo (quando ambos voam)"),
                ModifierOption("are_estereis",      "Ermos",        0, "Desertos, montanhas, geleiras, planícies"),
                ModifierOption("are_urbanas",       "Cidades",      0, "Ruas, telhados, prédios, aposentos"),
                ModifierOption("are_subterraneos",  "Subterrâneos", 0, "Cavernas, masmorras, esgotos"),
                ModifierOption("are_outro",         "Um único lugar",0, "Um local específico definido com o Mestre")
            )
        ),

        // ─── ARMADURA EXTRA (Modular) ─────────────────────────────────────────
        AdvantageItem(
            name = "Armadura Extra",
            cost = "Especial",
            description = "Dobra a Armadura (A) contra o(s) tipo(s) de dano escolhido(s). Energias: 1PT | Físicos: 2PT | Gerais: 3PT.",
            isModular = true,
            baseCostPt = 0,
            modifiers = listOf(
                ModifierOption("arm_fogo",          "Fogo",             1, "Dano de Fogo"),
                ModifierOption("arm_frio",          "Frio",             1, "Dano de Frio"),
                ModifierOption("arm_eletrico",      "Elétrico",         1, "Dano Elétrico"),
                ModifierOption("arm_quimico",       "Químico",          1, "Dano Químico"),
                ModifierOption("arm_sonico",        "Sônico",           1, "Dano Sônico"),
                ModifierOption("arm_corte",         "Corte",            2, "Dano por Corte"),
                ModifierOption("arm_perfuracao",    "Perfuração",       2, "Dano por Perfuração"),
                ModifierOption("arm_esmagamento",   "Esmagamento",      2, "Dano por Esmagamento"),
                ModifierOption("arm_magia",         "Magia",            3, "Qualquer dano causado por magia"),
                ModifierOption("arm_forca",         "Força",            3, "Ataques corporais focados em Força"),
                ModifierOption("arm_pdf",           "Poder de Fogo",    3, "Ataques à distância baseados em PdF")
            )
        ),

        // ─── ATAQUE ESPECIAL (Modular) ────────────────────────────────────────
        AdvantageItem(
            name = "Ataque Especial",
            cost = "1+ pontos",
            description = "Ataque elemental ou especial. Custo base: 1PT + modificadores escolhidos.",
            isModular = true,
            baseCostPt = 1,
            modifiers = listOf(
                ModifierOption("ae_amplo",          "Amplo (+2 pontos)",    2, "Atinge todos os alvos no alcance do ataque (+2 PMs)"),
                ModifierOption("ae_lento",          "Lento (-1 ponto)",    -1, "Apenas para ataques com PdF. Alvo recebe H+2 na esquiva"),
                ModifierOption("ae_paralisante",    "Paralisante",          1, "Além de dano, funciona como Vantagem Paralisia (+1 PM)"),
                ModifierOption("ae_penetrante",     "Penetrante",           1, "Impõe A-2 contra o alvo na sua Força de Defesa (+1 PM)"),
                ModifierOption("ae_perigoso",       "Perigoso",             1, "Acerto crítico com resultado 5 ou 6 no dado (+1 PM)"),
                ModifierOption("ae_perto_morte",    "Perto da Morte",      -2, "O ataque só pode ser usado quando você está Perto da Morte (-1 PM)"),
                ModifierOption("ae_poderoso",       "Poderoso",             1, "Em caso de crítico, o ataque triplica sua F ou PdF (+1 PM)"),
                ModifierOption("ae_preciso",        "Preciso",              1, "Impõe H-2 contra o alvo em sua Força de Defesa"),
                ModifierOption("ae_teleguiado",     "Teleguiado",           1, "Apenas para PdF. Ataque persegue impondo H-2 em esquivas")
            )
        ),

        AdvantageItem(name = "Ataque Múltiplo", cost = "1 ponto", description = "Permite fazer mais ataques corpo-a-corpo no turno. Cada ataque extra (além do primeiro) custa 1 PM."),
        AdvantageItem(name = "Boa Fama", cost = "1 ponto", description = "Você é respeitado e famoso. Pode ser mais difícil passar despercebido ou agir disfarçado."),
        AdvantageItem(name = "Clericato", cost = "1 ponto", description = "Acesso a Magia Branca (e outras definidas pelo mestre), com 3 magias extras iniciais."),

        AdvantageItem(name = "Deflexão", cost = "1 ponto", description = "2 PMs para duplicar a H na FD contra ataques de PdF."),
        AdvantageItem(name = "Elementalista", cost = "1 ponto (cada)", description = "Reduz custo de PM pela metade (arredondado para cima) em magias de um elemento."),
        AdvantageItem(name = "Energia Extra", cost = "1 ou 2 pontos", description = "Gasta 2 PMs para recuperar todos os PVs. 1 pt: Só funciona Perto da Morte. 2 pts: Qualquer momento."),
        AdvantageItem(name = "Energia Vital", cost = "2 pontos", description = "Gasta PVs em lugar de PMs (2 PVs = 1 PM). Apenas para magias e vantagens."),

        AdvantageItem(name = "Familiar", cost = "1 ponto", description = "Pequeno animal mágico que partilha Habilidades e Vantagens, com Ligação Natural."),
        AdvantageItem(name = "Forma Alternativa", cost = "2 pontos (cada)", description = "Transforma-se em outra forma com ficha própria. Demora um movimento."),
        AdvantageItem(name = "Genialidade", cost = "1 ponto", description = "Recebe H+2 para usar ou aprender Perícias, ou testes envolvendo perícias que não possua."),
        AdvantageItem(name = "Imortal", cost = "1 ou 2 pontos", description = "1pt: Retorna da morte após dias/meses. 2pts: Retorna após o combate."),
        AdvantageItem(name = "Inimigo", cost = "1 ponto (cada)", description = "H+2 em combate e em testes de perícias envolvendo um grupo de criaturas específico."),
        AdvantageItem(name = "Invisibilidade", cost = "2 pontos", description = "Fora de combate reduz testes de furtividade. Em combate impõe H-1 (C/C) ou H-3 (Longa). 1 PM/turno."),
        AdvantageItem(name = "Invulnerabilidade", cost = "Especial", description = "Dano de um tipo específico é dividido por dez. Só pode ser obtida em campanha/única."),
        AdvantageItem(name = "Ligação Natural", cost = "1 ponto", description = "Conexão especial com Aliado. Comunicação telepática visual; sentir emoções cegas."),
        AdvantageItem(name = "Magia Branca", cost = "2 pontos", description = "Acesso às Magias de cura ou defensivas."),
        AdvantageItem(name = "Magia Elemental", cost = "2 pontos", description = "Acesso às Magias baseadas nos quatro elementos."),
        AdvantageItem(name = "Magia Negra", cost = "2 pontos", description = "Acesso às Magias ligadas a morte e deterioração."),
        AdvantageItem(name = "Magia Irresistível", cost = "1 a 3 pontos", description = "Impõe redutor no teste de Resistência da vítima às magias. 1pt(-1), 2pts(-2), 3pts(-3)."),
        AdvantageItem(name = "Membros Elásticos", cost = "1 ponto", description = "Permite realizar ataques corporais normais a distância como se tivesse alcance de PdF."),
        AdvantageItem(name = "Membros Extras", cost = "1 ponto (cada)", description = "Permite um ataque extra ou bloqueio de FD+1 constante. Acumula os efeitos de Monstruoso."),
        AdvantageItem(name = "Memória Expandida", cost = "2 pontos", description = "Memória infalível. Aprende perícias ao observar (-1 perícia por vez). Não precisa testar aprender magias."),
        AdvantageItem(name = "Mentor", cost = "1 ponto", description = "Um mestre que fornece três magias extras, e responde a perguntas ou dá dicas telepaticamente."),


        AdvantageItem(name = "Paladino", cost = "1 ponto", description = "Guerreiro do bem. R+1 para PVs e PMs. Pode conjurar Cura Mágica e Detectar o Mal pelo custo normal em PMs."),
        AdvantageItem(name = "Paralisia", cost = "1 ponto", description = "2 PMs ou mais. Se causar dano, o alvo deve passar em teste de R ou ficar paralisado por (PMs gastos/2) turnos."),
        AdvantageItem(name = "Parceiro", cost = "1 ponto (cada)", description = "Agem como um só lutador, combinando características mais altas. Dano recebido é dividido igualmente."),
        AdvantageItem(name = "Patrono", cost = "1 ponto", description = "Grande organização. 1 PM invoca ajuda de acordo com a situação. Para magos, concede três magias extras."),
        AdvantageItem(name = "Poder Oculto", cost = "1 ponto", description = "Gasta 1 PM para aumentar uma característica em +1 (max +5, 1 turno/pto), ou todas (+2, max +10). Perde se atacado/atingir 0 PVs."),
        AdvantageItem(name = "Pontos de Magia Extras", cost = "1 ponto (cada)", description = "Soma R+2 no cálculo de PMs totais (não na Resistência real)."),
        AdvantageItem(name = "Pontos de Vida Extras", cost = "1 ponto (cada)", description = "Soma R+2 no cálculo de PVs totais (não na Resistência real)."),
        AdvantageItem(name = "Possessão", cost = "2 pontos", description = "Possui um corpo desacordado. Gasta PM igual à R do alvo ao entrar e a cada hora."),
        AdvantageItem(name = "Reflexão", cost = "2 pontos", description = "Gasta 2 PMs para duplicar H na FD e, se FD >= FA, devolve o ataque de PdF com a FA original."),
        AdvantageItem(name = "Regeneração", cost = "3 pontos", description = "Recupera 1 PV por turno. Altera resultados de testes de Morte (recupera PV mais rápido). Não afeta PMs."),
        AdvantageItem(name = "Resistência à Magia", cost = "1 ponto", description = "Concede +2 no teste de R para ignorar efeito de vantagem ou magia (exceto dano verdadeiro)."),
        AdvantageItem(name = "Riqueza", cost = "2 pontos", description = "Gasta 1 PM para ter ajuda como o dinheiro permitir. Nunca substitui Vantagens/Desvantagens."),


        // ─── SENTIDOS ESPECIAIS (Modular) ─────────────────────────────────────
        AdvantageItem(
            name = "Sentidos Especiais",
            cost = "1-2 pontos",
            description = "1 ponto: 3 sentidos à sua escolha. 2 pontos: todos os sentidos disponíveis na lista.",
            isModular = true,
            baseCostPt = 1,
            modifiers = listOf(
                ModifierOption("se_audicao",        "Audição Aguçada",      0, "Ouvir sons muito baixos ou distantes."),
                ModifierOption("se_faro",           "Faro Aguçado",         0, "Farejar como um perdigueiro."),
                ModifierOption("se_infra",          "Infravisão",           0, "Ver calor; fácil ver no escuro."),
                ModifierOption("se_radar",          "Radar",                0, "Perceber formas ao redor mesmo de olhos fechados."),
                ModifierOption("se_invisivel",      "Ver o Invisível",      0, "Ver coisas e seres invisíveis."),
                ModifierOption("se_visao_aguc",     "Visão Aguçada",        0, "Enxergar mais longe."),
                ModifierOption("se_raiosx",         "Visão de Raio X",      0, "Ver através de portas e paredes (exceto chumbo/mágicos).")
            )
        ),

        AdvantageItem(name = "Separação", cost = "2 pontos", description = "Gasta 4 PMs para invocar cópias exatas de si (máximo = Resistência). As cópias sofrem penalidade de -1 nas características."),
        AdvantageItem(name = "Telepatia", cost = "1 ponto", description = "2 PMs por uso: Lê pensamentos (H+2 Investigação/Lábia), Analisa poder, Revela tesouro, ou Prevê movimentos adversários (+1 Iniciativa/Esquiva)."),
        AdvantageItem(name = "Teleporte", cost = "2 pontos", description = "Gasta 1 PM para se teleportar para local visível (até 10m x Habilidade). Funciona como mover-se."),
        AdvantageItem(name = "Tiro Carregável", cost = "1 ponto", description = "Gasta 2 PMs e um turno inteiro concentrando para dobrar o PdF no ataque do turno seguinte. Fica indefeso durante concentração."),
        AdvantageItem(name = "Tiro Múltiplo", cost = "2 pontos", description = "Vários ataques de PdF em uma rodada. Cada disparo (incluindo o primeiro) consome 1 PM. Máximo de ataques = Habilidade."),
        AdvantageItem(name = "Toque de Energia", cost = "1 ponto (cada)", description = "Transmitir energia com FA = A + 1d para cada PM gasto. Máximo PMs igual à Armadura. Ignora Habilidade na FA."),
        AdvantageItem(name = "Torcida", cost = "1 ponto", description = "Se o público for a favor, ganha H+1 e impõe H-1 ao oponente (se ele falhar em teste de R)."),
        AdvantageItem(name = "Voo", cost = "2 pontos", description = "Permite voar. H1: levitar; H2: 20m/s; H3+: 40m/s, +40m/s por ponto de H extra. Teste de H para velocidade acima do limite."),
        AdvantageItem(name = "Xamã", cost = "1 ponto", description = "Forte ligação com o mundo dos espíritos. Pode interagir e atacar seres incorpóreos. Recebe Ver o Invisível para esse fim.")
    )
}
