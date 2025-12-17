package com.galeria.defensores.data

import com.galeria.defensores.models.UniqueAdvantage

object UniqueAdvantagesData {
    val defaults = listOf(
        UniqueAdvantage(
            "Humano", "Humanos", 0,
            "Não possui bônus, sendo o padrão.",
            "Não possui penalidades."
        ),
        UniqueAdvantage(
            "Anão", "Semi-Humanos", 1,
            "Infravisão, Resistência à Magia, Testes de Resistência +1, Inimigos (Orcs e Goblinoides).",
            "Não possui penalidades."
        ),
        UniqueAdvantage(
            "Elfo", "Semi-Humanos", 1,
            "Habilidade +1, Visão Aguçada, FA+1 com espada e arco, Aptidão para Magia Elemental.",
            "Não possui penalidades."
        ),
        UniqueAdvantage(
            "Elfo Negro", "Semi-Humanos", 2,
            "Habilidade +1, Infravisão, Resistência à Magia, Magia Branca ou Negra (grátis).",
            "Ponto Fraco (luz do dia)."
        ),
        UniqueAdvantage(
            "Gnomo", "Semi-Humanos", 2,
            "Habilidade +1, Genialidade, Faro Aguçado, Pequenos Desejos (grátis).",
            "Modelo Especial."
        ),
        UniqueAdvantage(
            "Halfling", "Semi-Humanos", 1,
            "Habilidade +1, Poder de Fogo +1, Aptidão para Crime.",
            "Modelo Especial."
        ),
        UniqueAdvantage(
            "Meio-Elfo", "Semi-Humanos", 0,
            "Visão Aguçada, Aptidão para Artes e Manipulação.",
            "Não possui penalidades."
        ),
        UniqueAdvantage(
            "Meio-Orc", "Semi-Humanos", 0,
            "Força +1, Infravisão.",
            "Má Fama, Proibido comprar Genialidade/Memória Expandida."
        ),
        UniqueAdvantage(
            "Alien", "Humanoides", 2,
            "Característica +1 (escolhida), Armadura Extra (energia, escolhida), Vantagem Bônus (1 pt).",
            "Inculto."
        ),
        UniqueAdvantage(
            "Anfíbio", "Humanoides", 0,
            "Resistência +1, Natação, Radar (na água).",
            "Ambiente Especial (água), Vulnerabilidade (Fogo)."
        ),
        UniqueAdvantage(
            "Centauro", "Humanoides", 1,
            "Força +1, Habilidade +1 (para corrida/fuga/perseguição), Combate Táurico (2 ataques por turno com patas).",
            "Modelo Especial."
        ),
        UniqueAdvantage(
            "Goblin", "Humanoides", -1,
            "Testes de Resistência +1, Infravisão, Aptidão para Crime.",
            "Má Fama, Magia (custo 3 pts para magias)."
        ),
        UniqueAdvantage(
            "Kemono", "Humanoides", 1,
            "Habilidade +1, Sentidos Especiais (2 à escolha).",
            "Não possui penalidades."
        ),
        UniqueAdvantage(
            "Meio-Dragão", "Humanoides", 4,
            "Arcano, Invulnerabilidade (elemento ligado ao pai).",
            "Não possui penalidades."
        ),
        UniqueAdvantage(
            "Minotauro", "Humanoides", 0,
            "Força +2, Resistência +1, Mente Labiríntica.",
            "Código de Honra do Combate, Má Fama, Fobia (altura)."
        ),
        UniqueAdvantage(
            "Ogre", "Humanoides", 2,
            "Força +3, Resistência +3.",
            "Modelo Especial, Inculto, Má Fama, Monstruoso, Vantagens Proibidas (Genialidade, Magia, etc.)."
        ),
        UniqueAdvantage(
            "Troglodita", "Humanoides", 2,
            "Força +1, Armadura +1, Infravisão, Camuflagem, Ataque Pestilento.",
            "Monstruoso, Vulnerabilidade (Frio)."
        ),
        UniqueAdvantage(
            "Anjo", "Youkai", 2,
            "Boa Fama, Sentidos Especiais (Infravisão, Visão Aguçada, Ver o Invisível), Invulnerabilidade (Elétrico e Sônico), Aptidão Voo e Magia Branca, Teleportação Planar.",
            "Maldição (banimento), Vulnerabilidade (Fogo)."
        ),
        UniqueAdvantage(
            "Demônio", "Youkai", 1,
            "Sentidos Especiais (Infravisão, Faro Aguçado, Ver o Invisível), Invulnerabilidade (Fogo), Aptidão Voo e Magia Negra, Teleportação Planar.",
            "Má Fama, Maldição (banimento), Vulnerabilidade (Elétrico e Sônico)."
        ),
        UniqueAdvantage(
            "Fada", "Youkai", 3,
            "Habilidade +1, Aparência Inofensiva, Voo, Magia (Branca ou Negra + Elemental).",
            "Modelo Especial, Vulnerabilidade (Magia)."
        ),
        UniqueAdvantage(
            "Licantropo", "Youkai", 0,
            "Força Animal (F+1 e A+1, dobro na forma de fera).",
            "Monstruoso (na forma de fera), Modelo Especial (na forma de fera), Vulnerabilidade (Magia e Prata na forma de fera), Transformação involuntária."
        ),
        UniqueAdvantage(
            "Meio-Abissal", "Youkai", 2,
            "Sentidos Especiais (Infravisão, Faro Aguçado, Ver o Invisível), Armadura Extra (Fogo), Aptidão Voo e Magia Negra.",
            "Vulnerabilidade (Elétrico e Sônico)."
        ),
        UniqueAdvantage(
            "Meio-Celestial", "Youkai", 2,
            "Sentidos Especiais (Infravisão, Visão Aguçada, Ver o Invisível), Armadura Extra (Elétrico e Sônico), Aptidão Voo e Magia Branca.",
            "Vulnerabilidade (Fogo)."
        ),
        UniqueAdvantage(
            "Meio-Gênio", "Youkai", 4,
            "Arcano, Armadura Extra (elemento), Aptidão Voo, Desejos (gasta metade PM para outros).",
            "Código da Gratidão."
        ),
        UniqueAdvantage(
            "Androide", "Construtos", 1,
            "Imunidades (sono, fome, mente, veneno, doença), Reparos (cura com Máquinas), Aparência Humana (opcional).",
            "Vulnerável a magias se tiver Alma Humana."
        ),
        UniqueAdvantage(
            "Ciborgue", "Construtos", 0,
            "Imunidades (mínimas), Construto Vivo (recupera metade dos PVs).",
            "Cérebro Orgânico (afetado por magias mentais/vivas)."
        ),
        UniqueAdvantage(
            "Golem", "Construtos", 3,
            "Imunidades, Armadura Extra (Magia), Camuflagem.",
            "Monstruoso."
        ),
        UniqueAdvantage(
            "Mecha", "Construtos", 0,
            "Imunidades, Aptidão Forma Alternativa, Reparos.",
            "Modelo Especial."
        ),
        UniqueAdvantage(
            "Meio-Golem", "Construtos", 1,
            "Imunidades (mínimas), Construto Vivo (recupera metade PVs), Ganha Magia (Branca, Elemental ou Negra).",
            "Cérebro Orgânico, Insano (Fobia)."
        ),
        UniqueAdvantage(
            "Nanomorfo", "Construtos", 3,
            "Imunidades, Doppleganger, Adaptador, Membros Elásticos, Bônus em Perícias (Crime, Máquinas), Regeneração, Aptidão Separação.",
            "Não possui penalidades."
        ),
        UniqueAdvantage(
            "Robô Positrônico", "Construtos", -2,
            "Imunidades.",
            "Código de Honra (Três Leis da Robótica)."
        ),
        UniqueAdvantage(
            "Esqueleto", "Mortos-Vivos", 0,
            "Imunidades, Infravisão, Energia Negativa, Invulnerabilidade (Frio), Armadura Extra (Corte e Perfuração).",
            "Devoção, Inculto, Monstruoso."
        ),
        UniqueAdvantage(
            "Fantasma", "Mortos-Vivos", 3,
            "Imunidades, Infravisão, Energia Negativa, Incorpóreo, Imortal, Pânico (grátis), Aptidão Invisibilidade e Possessão.",
            "Devoção."
        ),
        UniqueAdvantage(
            "Múmia", "Mortos-Vivos", 3,
            "Imunidades, Infravisão, Energia Negativa, Armadura Extra (todos, exceto Fogo/Magia), Podridão de Múmia, Pânico (grátis).",
            "Ambiente Especial."
        ),
        UniqueAdvantage(
            "Vampiro", "Mortos-Vivos", -1,
            "Imunidades, Infravisão, Energia Negativa.",
            "Dependência (sangue/vida), Maldição (sol)."
        ),
        UniqueAdvantage(
            "Zumbi", "Mortos-Vivos", -2,
            "Imunidades, Infravisão, Energia Negativa.",
            "Dependência (carne/órgão), Lentidão, Inculto, Monstruoso."
        )
    )
}
