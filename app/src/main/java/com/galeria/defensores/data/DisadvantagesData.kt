package com.galeria.defensores.data

import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.models.ModifierOption

object DisadvantagesData {
    val defaultDisadvantages = listOf(
        AdvantageItem(name = "Ambiente Especial", cost = "-1 a -2 pontos", description = "Depende de um ambiente específico para manter sua força vital."),
        AdvantageItem(name = "Assombrado", cost = "-1 a -2 pontos", description = "Perseguido por uma presença constante que impõe redutores em combate."),
        AdvantageItem(name = "Bateria", cost = "-1 ponto", description = "Depende de energia para funcionar; sofre redutores ao esgotar a carga."),

        // ─── COMPLEXO (Modular) ────────────────────────────────────────────────
        AdvantageItem(
            name = "Complexo",
            cost = "-1 a -2 pontos",
            description = "Traços de personalidade que dificultam a convivência. Escolha um ou mais Complexos.",
            isModular = true,
            baseCostPt = -1,
            modifiers = listOf(
                ModifierOption("cx_amnesia",        "Amnésia",                  0, "Não lembra nada do passado"),
                ModifierOption("cx_antissocial",    "Antissocial",              0, "-1 em testes na presença de desconhecidos"),
                ModifierOption("cx_atrapalhado",    "Atrapalhado",              0, "-1 em Perícias; dobra tempo para agir sem redutor"),
                ModifierOption("cx_consumista",     "Consumista",               0, "Perde 1 moeda sempre que chega em uma cidade"),
                ModifierOption("cx_culpa",          "Complexo de Culpa",        0, "-1 em testes por 2d Cenas ao se sentir culpado"),
                ModifierOption("cx_covarde",        "Covarde",                  0, "-1 na FD"),
                ModifierOption("cx_curioso",        "Curioso",                  0, "Testa R ao ver algo inesperado ou tentador"),
                ModifierOption("cx_desorientado",   "Desorientado",             0, "Testa H em estresse; falha = indefeso por 1 turno"),
                ModifierOption("cx_diurno",         "Diurno",                   0, "-1 em todos os testes à noite"),
                ModifierOption("cx_ganancioso",     "Ganancioso",               0, "Testa R para não aceitar ofertas vantajosas"),
                ModifierOption("cx_gatilho",        "Gatilho",                  0, "Certo estímulo causa Atordoamento por 1 turno"),
                ModifierOption("cx_gregario",       "Gregário",                 0, "-1 em testes quando fora do alcance de aliados"),
                ModifierOption("cx_novato",         "Novato",                   0, "Testa H para usar Poder; -1 em Ocultismo"),
                ModifierOption("cx_preguicoso",     "Preguiçoso",               0, "Gasta PM para entrar em combate"),
                ModifierOption("cx_sanguinario",    "Sanguinário",              0, "Deve matar oponentes derrotados ou sofrer -1"),
                ModifierOption("cx_tapado",         "Tapado",                   0, "Testa R em situações de bom senso"),
                ModifierOption("cx_trauma",         "Trauma",                   0, "Paralisa ao rever o gatilho traumático"),
                ModifierOption("cx_viciado",        "Viciado",                  0, "Sem o vício: -1 em testes por PRD")
            )
        ),

        // ─── CÓDIGO (Modular) ──────────────────────────────────────────────────
        AdvantageItem(
            name = "Código",
            cost = "-1 ponto (cada)",
            description = "Princípios rígidos de conduta. Violar um Código custa 1 PD.",
            isModular = true,
            baseCostPt = -1,
            modifiers = listOf(
                ModifierOption("co_abstinencia",    "Abstinência",      0, "Jamais consumir carne, álcool ou atos carnais"),
                ModifierOption("co_cacador",        "Caçador",          0, "Jamais matar fêmeas grávidas; sempre a presa mais perigosa"),
                ModifierOption("co_cavalheiro",     "Cavalheiro",       0, "Nunca atacar mulheres; sempre atendê-las"),
                ModifierOption("co_combate",        "Combate",          0, "Nunca atacar inimigos indefesos ou em menor número"),
                ModifierOption("co_derrota",        "Derrota",          0, "Nunca aceitar rendição"),
                ModifierOption("co_desafio",        "Desafio",          0, "Jamais recusar desafios"),
                ModifierOption("co_gratidao",       "Gratidão",         0, "Deve servir quem lhe salvou a vida"),
                ModifierOption("co_guerra",         "Guerra",           0, "Jamais recuar ou se curar com magia"),
                ModifierOption("co_herois",         "Heróis",           0, "Jamais mentir ou quebrar promessas"),
                ModifierOption("co_hierarquia",     "Hierarquia",       0, "Jamais desobedecer autoridades reconhecidas"),
                ModifierOption("co_honestidade",    "Honestidade",      0, "Jamais mentir, roubar ou trapacear"),
                ModifierOption("co_mascara",        "Máscara",          0, "Sempre cobrir o rosto; nunca revelar identidade"),
                ModifierOption("co_ninja",          "Ninja",            0, "Jamais abandonar uma missão"),
                ModifierOption("co_pacifista",      "Pacifista",        0, "Nunca inicia combate; só causa dano não letal"),
                ModifierOption("co_protetor",       "Protetor",         0, "Jamais prejudicar membros de uma ancestralidade"),
                ModifierOption("co_rebeldia",       "Rebeldia",         0, "Jamais aceitar ordens sem questionar"),
                ModifierOption("co_redencao",       "Redenção",         0, "Jamais atacar primeiro; aceitar rendição"),
                ModifierOption("co_samaritano",     "Samaritano",       0, "Jamais ferir nenhum ser vivo"),
                ModifierOption("co_selva",          "Selva",            0, "Jamais usar metal; nunca atacar animais"),
                ModifierOption("co_trapaceiro",     "Trapaceiro",       0, "Jamais recusar oportunidade de enganar alguém")
            )
        ),

        AdvantageItem(name = "Corpo Artificial", cost = "-1 a -2 pontos", description = "Corpo inorgânico; imune a doenças mas só recupera PV mecanicamente."),

        // ─── DEBILITAÇÃO (Modular) ─────────────────────────────────────────────
        AdvantageItem(
            name = "Debilitação",
            cost = "-1 a -3 pontos",
            description = "Limitação física ou sensorial. Escolha a debilitação e sua gravidade.",
            isModular = true,
            baseCostPt = -1,
            modifiers = listOf(
                ModifierOption("db_audicao",     "Audição Ruim (H)",     0, "Redutor em percepção auditiva"),
                ModifierOption("db_surdez",      "Surdez (H)",           0, "-3; não realiza testes de som"),
                ModifierOption("db_desmembrado", "Desmembrado (H)",      0, "Ausência de membro com penalidade variável"),
                ModifierOption("db_fragilidade", "Fragilidade (A)",      0, "Vulnerável a dano; redutor no turno seguinte"),
                ModifierOption("db_hemofilia",   "Hemofilia (R)",        0, "Sangramento contínuo ao receber dano"),
                ModifierOption("db_locomocao",   "Locomoção (H)",        0, "Dificuldade de movimento; sob Lentidão sem auxílio"),
                ModifierOption("db_visao_ruim",  "Visão Ruim (H)",       0, "-1 a -2 em percepção visual"),
                ModifierOption("db_cegueira",    "Cegueira (H)",         0, "-3; pode ser reduzido para -2 com Radar"),
                ModifierOption("db_mudez",       "Mudez (H)",            0, "Incapaz de comunicação verbal"),
                ModifierOption("db_grave",       "Nível Grave (+1PT)",   0, "Eleva o custo e o efeito da debilitação")
            )
        ),

        AdvantageItem(name = "Dependência", cost = "-1 a -2 pontos", description = "Precisa consumir algo raro diariamente."),
        AdvantageItem(name = "Devoção", cost = "-1 a -2 pontos", description = "Obcecado por missão; sofre redutor agindo contrariamente."),
        AdvantageItem(name = "Fetiche", cost = "-1 a -2 pontos", description = "Precisa de objeto para canalizar poderes."),
        AdvantageItem(name = "Fraqueza", cost = "-1 a -3 pontos", description = "Perde PVs e PMs em determinada condição."),
        AdvantageItem(name = "Furioso", cost = "-1 ponto", description = "Em certa condição pode entrar em fúria incontrolável."),
        AdvantageItem(name = "Inaptidão", cost = "-2 a -3 pontos", description = "Incapaz de interagir com o sistema de Poder do cenário."),
        AdvantageItem(name = "Inculto", cost = "-1 ponto", description = "Dificuldades de comunicação; -1d em testes externos."),

        // ─── INSANO (Modular) ──────────────────────────────────────────────────
        AdvantageItem(
            name = "Insano",
            cost = "-2 a -3 pontos",
            description = "Distúrbio mental grave. -2 em testes sociais ao ser descoberto.",
            isModular = true,
            baseCostPt = -2,
            modifiers = listOf(
                ModifierOption("ins_ansioso",       "Ansioso",              0, "Ao entrar em combate, chance de Medo por 1 Cena"),
                ModifierOption("ins_cleptomaniaco", "Cleptomaníaco",        0, "Testa R para não roubar; nunca devolve"),
                ModifierOption("ins_compulsivo",    "Compulsivo",           0, "Deve realizar ação específica a cada hora"),
                ModifierOption("ins_depressivo",    "Depressivo",           0, "1/6 de chance de perder a Ação por apatia"),
                ModifierOption("ins_fobia_suave",   "Fobia Suave",          0, "Medo paralisante de algo 25% do tempo"),
                ModifierOption("ins_histerico",     "Histérico",            0, "Crises emocionais em situações de estresse"),
                ModifierOption("ins_insonia",       "Insônia",              0, "Só dorme em falha; acorda cansado"),
                ModifierOption("ins_mentiroso",     "Mentiroso",            0, "Nunca diz a verdade"),
                ModifierOption("ins_narcoleptico",  "Narcoléptico",         0, "1/6 de chance de dormir involuntariamente"),
                ModifierOption("ins_paranoico",     "Paranoico",            0, "Não confia em ninguém; recusa ajuda"),
                ModifierOption("ins_sadico",        "Sádico",               0, "Precisa causar dor diariamente"),
                ModifierOption("ins_fobia_grave",   "Fobia Grave",          0, "Medo paralisante de algo 50% do tempo"),
                ModifierOption("ins_homicida",      "Homicida",             0, "Deve matar alguém da própria espécie periodicamente"),
                ModifierOption("ins_megalomaniaco", "Megalomaníaco",        0, "Acredita ser invencível; jamais recua"),
                ModifierOption("ins_suicida",       "Suicida",              0, "Busca a morte ativamente"),
                ModifierOption("ins_multipla",      "Múltipla Personalidade", 0, "Duas fichas distintas que alternam controle"),
                ModifierOption("ins_grave",         "Insanidade Grave (+1PT)", 0, "Classifica esta insanidade como Grave")
            )
        ),

        AdvantageItem(name = "Má Fama", cost = "-1 a -2 pontos", description = "Infame; -1 em testes sociais."),

        // ─── MALDIÇÃO (Modular) ────────────────────────────────────────────────
        AdvantageItem(
            name = "Maldição",
            cost = "-1 ponto",
            description = "Sofre uma maldição persistente. Escolha o tipo.",
            isModular = true,
            baseCostPt = -1,
            modifiers = listOf(
                ModifierOption("mal_barulho",    "Barulho Incômodo",         0, "Presença acompanhada por som irritante"),
                ModifierOption("mal_chuva",      "Chuva Ambulante",          0, "Nuvem de chuva te segue"),
                ModifierOption("mal_genero",     "Gênero Invertido",         0, "Transformado na versão do gênero oposto"),
                ModifierOption("mal_agua",       "Água Corrente",            0, "Incapaz de atravessar cursos d'água corrente"),
                ModifierOption("mal_azarado",    "Atrasado",                 0, "Iniciativa-2 por atraso inevitável"),
                ModifierOption("mal_fedido",     "Fedido",                   0, "-2 em testes sociais; todos testam R para permanecer"),
                ModifierOption("mal_maoR",       "Mão Rebelde",              0, "Mão tem vontade própria; H-1 em tensão"),
                ModifierOption("mal_marcaM",     "Marca do Mentiroso",       0, "Sinal visível ao mentir"),
                ModifierOption("mal_pesadelos",  "Pesadelos",                0, "Perde PV ao dormir; não recupera PV com descanso"),
                ModifierOption("mal_karma",      "Karma",                    0, "Todo mal retorna com efeito idêntico"),
                ModifierOption("mal_recuperImp", "Recuperação Impossível",   0, "Impossível recuperar PV ou PM")
            )
        ),

        AdvantageItem(name = "Modelo Especial", cost = "-2 pontos", description = "Corpo anormal; não usa equipamentos padrão."),
        AdvantageItem(name = "Monstruoso", cost = "-1 a -2 pontos", description = "Aparência repulsiva; -1 em testes sociais."),
        AdvantageItem(name = "Munição Limitada", cost = "-1 ponto", description = "Munição limitada; sem reabastecimento não pode usar PdF."),

        // ─── PODER DEFEITUOSO (Modular) ────────────────────────────────────────
        AdvantageItem(
            name = "Poder Defeituoso",
            cost = "-1 ponto",
            description = "Poderes instáveis ou com falhas. Escolha um defeito.",
            isModular = true,
            baseCostPt = -1,
            modifiers = listOf(
                ModifierOption("pd_agradavel",     "Agradável",        0, "Efeitos visuais alegres; +1 em A e R dos adversários"),
                ModifierOption("pd_constrangedor", "Constrangedor",    0, "Exige frases ridículas e gestos; FA-2"),
                ModifierOption("pd_descontrolado", "Descontrolado",    0, "1x/combate: poder aleatório sem gasto de PM"),
                ModifierOption("pd_desgastante",   "Desgastante",      0, "Todos os poderes custam +1PM"),
                ModifierOption("pd_exaustivo",     "Exaustivo",        0, "Perde a Ação no turno seguinte ao usar poder"),
                ModifierOption("pd_falho",         "Falho",            0, "1/6 de chance de falha ao usar poder"),
                ModifierOption("pd_impreciso",     "Impreciso",        0, "Não afeta alvos com H igual ou maior"),
                ModifierOption("pd_limitado",      "Limitado",         0, "Cada Vantagem só usada 3x por PRD"),
                ModifierOption("pd_morte",         "Perto da Morte",   0, "Só usa poderes com PV ≤ 1/4"),
                ModifierOption("pd_repetitivo",    "Repetitivo",       0, "Deve repetir o poder nas próximas 2 Ações"),
                ModifierOption("pd_seletivo",      "Seletivo",         0, "Poder afeta apenas seres de uma ancestralidade"),
                ModifierOption("pd_vingativo",     "Vingativo",        0, "Perde 1PV ao usar qualquer poder")
            )
        ),

        AdvantageItem(name = "Ponto Fraco", cost = "-1 ponto", description = "Fraqueza tática explorável; oponentes recebem +1 H e +2 FA."),
        AdvantageItem(name = "Protegido Indefeso", cost = "-1 a -2 pontos", description = "Deve defender alguém; penalidades quando em perigo."),
        AdvantageItem(name = "Restrição", cost = "-1 a -3 pontos", description = "Custo de PM dobrado em certas condições."),
        AdvantageItem(name = "Rival", cost = "-1 ponto", description = "Rival com mesmo PT aparece ao menos uma vez por sessão."),

        // ─── VULNERÁVEL (Modular) ──────────────────────────────────────────────
        AdvantageItem(
            name = "Vulnerável",
            cost = "-1 a -3 pontos",
            description = "Dano dobrado de um tipo específico (após FD). Energias: -1PT | Físicos: -2PT | Gerais: -3PT.",
            isModular = true,
            baseCostPt = 0,
            modifiers = listOf(
                ModifierOption("vul_acido",     "Ácido (-1PT)",         -1, "Dano de Ácido dobrado"),
                ModifierOption("vul_eletrico",  "Elétrico (-1PT)",      -1, "Dano Elétrico dobrado"),
                ModifierOption("vul_fogo",      "Fogo (-1PT)",          -1, "Dano de Fogo dobrado"),
                ModifierOption("vul_frio",      "Frio (-1PT)",          -1, "Dano de Frio dobrado"),
                ModifierOption("vul_luz",       "Luz (-1PT)",           -1, "Dano de Luz dobrado"),
                ModifierOption("vul_psiquico",  "Psíquico (-1PT)",      -1, "Dano Psíquico dobrado"),
                ModifierOption("vul_quimico",   "Químico (-1PT)",       -1, "Dano Químico dobrado"),
                ModifierOption("vul_sonico",    "Sônico (-1PT)",        -1, "Dano Sônico dobrado"),
                ModifierOption("vul_trevas",    "Trevas (-1PT)",        -1, "Dano de Trevas dobrado"),
                ModifierOption("vul_veneno",    "Veneno (-1PT)",        -1, "Dano de Veneno dobrado"),
                ModifierOption("vul_lacerante", "Lacerante (-2PT)",     -2, "Dano Lacerante dobrado"),
                ModifierOption("vul_contusao",  "Contusão (-2PT)",      -2, "Dano por Contusão dobrado"),
                ModifierOption("vul_f",         "F - Força (-3PT)",     -3, "Ataques de Força dobrados"),
                ModifierOption("vul_pdf",       "PDF (-3PT)",           -3, "Ataques de PdF dobrados"),
                ModifierOption("vul_poder",     "Poder (-3PT)",         -3, "Dano de Poder dobrado")
            )
        )
    )
}
