package com.galeria.defensores.data

import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.models.ModifierOption

object DisadvantagesData {
    val defaultDisadvantages = listOf(
        AdvantageItem(name = "Ambiente Especial", cost = "-1 ponto", description = "Fora do ambiente nativo (após R dias), perde 1 Força e 1 Resistência por dia. Recupera com 24h no ambiente ou 1 PE."),
        AdvantageItem(name = "Assombrado", cost = "-2 pontos", description = "Um fantasma atrapalha em combate (resultados 4-6 num dado impõem -1 em todas as características). Usa dobro de PM para magias."),
        AdvantageItem(name = "Bateria", cost = "-1 ponto", description = "Reserva de energia: 2 horas por ponto de R. Depois perde 1 Força e 1 Resistência por hora até 'desligar'."),

        // ─── CÓDIGO (Modular) ──────────────────────────────────────────────────
        AdvantageItem(
            name = "Código de Honra",
            cost = "-1 ponto (cada)",
            description = "Princípios rígidos de conduta. Violar um Código custa 1 PD.",
            isModular = true,
            baseCostPt = -1,
            modifiers = listOf(
                ModifierOption("co_abstinencia",    "1ª Lei de Asimov", 0, "Jamais causar mal a um ser humano, ou permitir que sofra mal"),
                ModifierOption("co_cacador",        "2ª Lei de Asimov", 0, "Sempre obedecer ordens de humanos, exceto quando violam outros códigos"),
                ModifierOption("co_arena",          "Arena",            0, "Nunca lutar fora dos seus terrenos escolhidos"),
                ModifierOption("co_cavalheiro",     "Caçador",          0, "Nunca matar/capturar fêmeas grávidas/filhotes; nunca abandonar caça"),
                ModifierOption("co_combate",        "Cavalheiro",       0, "Nunca atacar mulheres; sempre atender pedido de ajuda de uma mulher"),
                ModifierOption("co_derrota",        "Combate",          0, "Nunca atacar oponentes indefesos ou em desvantagem numérica"),
                ModifierOption("co_desafio",        "Derrota",          0, "Nunca aceitar derrota; deve lutar até 0 PVs e se matar se capturado"),
                ModifierOption("co_gratidao",       "Gratidão",         0, "Se for salvo, deve servir o salvador até devolver o favor"),
                ModifierOption("co_guerra",         "Heróis",           0, "Sempre cumprir palavras, proteger quem precisa, nunca recusar pedido de ajuda"),
                ModifierOption("co_herois",         "Honestidade",      0, "Nunca roubar, trapacear, mentir, desobedecer a leis"),
                ModifierOption("co_hierarquia",     "Redenção",         0, "Jamais atacar sem provocação; sempre aceitar rendição")
            )
        ),


        // ─── DEBILITAÇÃO (Modular) ─────────────────────────────────────────────
        AdvantageItem(
            name = "Deficiência Física",
            cost = "-1 a -3 pontos",
            description = "Limitação física ou sensorial. Escolha a debilitação e sua gravidade.",
            isModular = true,
            baseCostPt = -1,
            modifiers = listOf(
                ModifierOption("db_audicao",     "Audição Ruim",     0, "-1 para notar inimigos escondidos"),
                ModifierOption("db_cego",        "Cego",            -2, "-1 ataques corpo-a-corpo, -3 longa distância e esquivas"),
                ModifierOption("db_mudo",        "Mudo",            -1, "Incapaz de falar; não conjura magias faladas; testes sociais difíceis"),
                ModifierOption("db_surdo",       "Surdo",           -1, "-1 para notar inimigos. Não sabe se há alguém furtivo usando audição"),
                ModifierOption("db_semfaro",     "Sem Faro",         0, "Não sente cheiro nem gosto"),
                ModifierOption("db_visao",       "Visão Ruim",       0, "Míope ou caolho; -1 para esquivas e à distância; -1 para inimigos escondidos")
            )
        ),

        AdvantageItem(name = "Dependência", cost = "-2 pontos", description = "Precisa consumir ou fazer algo diariamente. Se não fizer, -1 R (ou -1 Habilidade). Morte/virar lenha em R dias."),
        AdvantageItem(name = "Devoção", cost = "-1 ponto", description = "Dedicado a um dever sagrado ou missão. Sem poder persegui-la, tem -1 em todas as características."),
        AdvantageItem(name = "Fetiche", cost = "-1 ponto", description = "Precisa de um objeto para canalizar poderes ou magia. Se perder não usa magia. Fetiche quebrado faz magia custar dobro de PMs."),
        AdvantageItem(name = "Fúria", cost = "-1 ponto", description = "Sempre que sofrer dano, testa R. Se falhar ataca a fonte incansavelmente e não usa PMs. Quando passa, sofre -1 em características por 1h."),
        AdvantageItem(name = "Inculto", cost = "-1 ponto", description = "Dificuldades de comunicação verbal. Outros têm muita dificuldade em fazê-lo conversar (testes muito difíceis)."),

        // ─── INSANO (Modular) ──────────────────────────────────────────────────
        AdvantageItem(
            name = "Insano",
            cost = "0 a -3 pontos",
            description = "Distúrbio mental. Apenas personagens com Aliado/Patrono/etc continuam confiando em você. -1 extra (cumulativo) em testes com o que não deseja fazer.",
            isModular = true,
            baseCostPt = 0,
            modifiers = listOf(
                ModifierOption("ins_distraido",     "Distraído",            0, "-1 naquilo que não quer fazer"),
                ModifierOption("ins_dupla",         "Dupla Personalidade",  0, "Duas fichas idênticas que mudam de pele e comportamento quando em perigo (4,5,6 num dado/hora)"),
                ModifierOption("ins_clepto",        "Cleptomaníaco",       -1, "Rouba por ser interessante. R para evitar. Devolve apenas se pego"),
                ModifierOption("ins_compulsivo",    "Compulsivo",          -1, "Ação diária > 1h para satisfazer ou não faz mais nada além de R/hora"),
                ModifierOption("ins_demente",       "Demente",             -1, "Igual Inculto"),
                ModifierOption("ins_fantasia",      "Fantasia",            -1, "Acredita ser algo que não é"),
                ModifierOption("ins_fobia_1pt",     "Fobia (-1 pt)",       -1, "Medo incomum. Coisas de encontro 25% das vezes"),
                ModifierOption("ins_furia",         "Fúria",               -1, "Igual à desvantagem"),
                ModifierOption("ins_depressivo",    "Depressivo",          -2, "Igual a Assombrado"),
                ModifierOption("ins_fobia_2pts",    "Fobia (-2 pts)",      -2, "Medo comum. Coisas de encontro 50% das vezes"),
                ModifierOption("ins_histerico",     "Histérico",           -2, "Igual a Assombrado"),
                ModifierOption("ins_homicida",      "Homicida",            -2, "Precisa matar humanoide/humano cada 1d dias"),
                ModifierOption("ins_megalomaniaco", "Megalomaníaco",       -1, "Acredita ser invencível; nunca recusa desafio ou foge"),
                ModifierOption("ins_mentiroso",     "Mentiroso",           -1, "Nunca diz a verdade (R pode vencer momentaneamente)"),
                ModifierOption("ins_obsessivo",     "Obsessivo",           -1, "Igual a Devoção"),
                ModifierOption("ins_paranoico",     "Paranoico",           -1, "Não confia em ninguém. Não dorme direito (recupera como se estivesse em lugar inadequado)"),
                ModifierOption("ins_sonambulo",     "Sonâmbulo",            0, "1,2,3 no dado ao dormir = anda dormindo. Acorda ao sofrer dano"),
                ModifierOption("ins_suicida",       "Suicida",              0, "Não dá valor à vida, sempre busca oportunidades de morrer"),
                ModifierOption("ins_fobia_3pts",    "Fobia (-3 pts)",      -3, "Medo diário. Quase o tempo todo")
            )
        ),

        AdvantageItem(name = "Interferência", cost = "0 pontos", description = "Emite campo constante que atrapalha comunicação rádio e certos aparelhos (10m por PV)."),
        AdvantageItem(name = "Interferência Mágica", cost = "0 pontos", description = "Aura antimágica a até 10m. Se rodar 1 ou 2 no dado a magia falha (PM gasto mesmo assim)."),
        AdvantageItem(name = "Má Fama", cost = "-1 ponto", description = "Infame. Todos desconfiam de você, e você é logo acusado quando há suspeitos."),

        // ─── MALDIÇÃO (Modular) ────────────────────────────────────────────────
        AdvantageItem(
            name = "Maldição",
            cost = "-1 a -2 pontos",
            description = "Uma sina perturbadora diária.",
            isModular = true,
            baseCostPt = 0,
            modifiers = listOf(
                ModifierOption("mal_suave", "Suave (-1 pt)", -1, "Irritante mas não causa penalidade (ex: virar mulher se molhado)."),
                ModifierOption("mal_grave", "Grave (-2 pts)", -2, "Coloca vida em risco (ex: vira porquinho e suas características caem a 0).")
            )
        ),

        AdvantageItem(name = "Modelo Especial", cost = "-1 ponto", description = "Corpo diferente do padrão humanoide. Não pode usar armas, roupas ou máquinas normais."),
        AdvantageItem(name = "Monstruoso", cost = "-1 ponto", description = "Aparência repulsiva e assustadora. Não pode andar pelas ruas disfarçado sem causar pânico."),
        AdvantageItem(name = "Munição Limitada", cost = "-1 ponto", description = "Pode atirar apenas 3 vezes seu valor de Poder de Fogo antes de precisar recarregar pagando."),

        // ─── PODER VERGONHOSO (Modular) ────────────────────────────────────────
        AdvantageItem(
            name = "Poder Vergonhoso",
            cost = "0 a -1 ponto",
            description = "Algo estranho ou embaraçoso acontece ao usar poderes que custem PMs.",
            isModular = true,
            baseCostPt = 0,
            modifiers = listOf(
                ModifierOption("pv_agradavel",     "Agradável (-1 pt)",      -1, "Ilusões bonitas não afetam mechas. Alvo ganha A+1 e R+1 para resistir"),
                ModifierOption("pv_constrange",    "Constrangedor (-1 pt)",  -1, "Atos vergonhosos, danças, magias com FA-1"),
                ModifierOption("pv_exagerado",     "Exagerado (-1 pt)",      -1, "Efeitos fantásticos e música alta. Alvo ganha R+1; magia demora 1 turno extra"),
                ModifierOption("pv_hentai",        "Hentai (0 pts)",          0, "Roupas da vítima desaparecem ou ficam transparentes. Redutor de H-1 na vítima se falha em R")
            )
        ),

        AdvantageItem(name = "Poder Vingativo", cost = "-1 ponto", description = "Toda magia ou poder que consuma PMs causa 1 ponto de dano direto a você."),
        AdvantageItem(name = "Ponto Fraco", cost = "-1 ponto", description = "Fraqueza tática. Inimigo que descobrir (H>1 ao assistir lutando) ganha H+1 contra você."),
        AdvantageItem(name = "Protegido Indefeso", cost = "-1 ponto (cada)", description = "Deve proteger alguém com a vida. Penalidades (H-1 cumulativo) se protegido estiver em perigo."),
        
        AdvantageItem(
            name = "Restrição de Poder",
            cost = "-1 a -3 pontos",
            description = "Em certa condição, usa 2x PMs nas magias e vantagens.",
            isModular = true,
            baseCostPt = 0,
            modifiers = listOf(
                ModifierOption("rp_incomum", "Incomum (-1 pt)", -1, "Condição rara (25% do tempo, ex: molhado)"),
                ModifierOption("rp_comum", "Comum (-2 pts)", -2, "Condição comum (50% do tempo, ex: durante dia/noite)"),
                ModifierOption("rp_muito", "Muito Comum (-3 pts)", -3, "Quase sempre (olhando p/ criaturas vivas, objetos)"),
            )
        ),



        // ─── VULNERÁVEL (Modular) ──────────────────────────────────────────────
        AdvantageItem(
            name = "Vulnerabilidade",
            cost = "Especial",
            description = "Não dá pontos nem é escolhida no começo. Reduz A a zero para calcular FD na vulnerabilidade.",
            isModular = true,
            baseCostPt = 0,
            modifiers = listOf(
                ModifierOption("vul_acido",     "Químico / Ácido",       0, "Vulnerável a Ácido/Químicos"),
                ModifierOption("vul_eletrico",  "Elétrico",              0, "Vulnerável Elétrico"),
                ModifierOption("vul_fogo",      "Fogo/Calor",            0, "Vulnerável a Fogo"),
                ModifierOption("vul_frio",      "Frio/Gelo",             0, "Vulnerável a Frio"),
                ModifierOption("vul_luz",       "Luz",                   0, "Vulnerável a Luz"),
                ModifierOption("vul_sonico",    "Sônico",                0, "Vulnerável Sônico"),
                ModifierOption("vul_trevas",    "Trevas",                0, "Vulnerável Trevas"),
                ModifierOption("vul_magia",     "Magia",                 0, "Vulnerável a Magia"),
                ModifierOption("vul_corte",     "Corte",                 0, "Vulnerável Lacerante"),
                ModifierOption("vul_esmag",     "Esmagamento",           0, "Vulnerável Contusão"),
                ModifierOption("vul_perfuracao","Perfuração",            0, "Vulnerável Perfuração")
            )
        )
    )
}
