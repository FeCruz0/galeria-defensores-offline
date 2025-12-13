package com.galeria.defensores.data

import com.galeria.defensores.models.AdvantageItem

object SkillsData {
    val defaultSkills = listOf(
        AdvantageItem(name = "Animais", cost = "2 pontos", description = "Permite o uso de habilidades como Doma, Montaria, Tratamento e Treinamento de animais. O personagem é capaz de lidar com criaturas de forma geral, curar ferimentos de animais (Veterinária) e instruí-los."),
        AdvantageItem(name = "Arte", cost = "2 pontos", description = "Engloba talentos artísticos como Atuação, Canto, Dança, Instrumentos Musicais, Redação, e Falsificação (documentos, arte). Também inclui a habilidade de realizar truques de mão (Prestidigitação)."),
        AdvantageItem(name = "Ciência", cost = "2 pontos", description = "Permite ao personagem realizar testes baseados em conhecimento em áreas acadêmicas vastas, como Astronomia, Biologia, Geografia, História, Meteorologia e Psicologia. Essencial para pesquisa e raciocínio lógico."),
        AdvantageItem(name = "Crime", cost = "2 pontos", description = "Habilidades para atividades ilícitas ou sorrateiras, como Arrombamento (abrir fechaduras), Furtividade (agir sem ser visto/ouvido), Punga (roubar), Rastreio (seguir pegadas) e criar/desarmar Armadilhas."),
        AdvantageItem(name = "Esporte", cost = "2 pontos", description = "Habilidades atléticas diversas. Inclui Acrobacia, Alpinismo, Corrida, Mergulho, Natação e Pilotagem. Confere um bônus de H+1 em testes de H que envolvam o uso do corpo (saltar, equilibrar-se, etc.)."),
        AdvantageItem(name = "Idiomas", cost = "2 pontos", description = "Torna o personagem um Poliglota, permitindo que ele fale, leia e escreva qualquer língua humana. Também abrange a decifração de códigos (Criptografia) e Leitura Labial."),
        AdvantageItem(name = "Investigação", cost = "2 pontos", description = "Habilidades para descobrir informações, seguir rastros (Rastreio) e obter a verdade de outras pessoas. Inclui Intimidação, Interrogatório, e a localização de Armadilhas ou objetos ocultos."),
        AdvantageItem(name = "Máquinas", cost = "2 pontos", description = "Permite operar, consertar e construir artefatos tecnológicos e veículos. Engloba Computação, Eletrônica, Engenharia, Mecânica, Condução e Pilotagem de veículos complexos."),
        AdvantageItem(name = "Manipulação", cost = "2 pontos", description = "Permite usar a lábia e a inteligência social para convencer e influenciar os outros (Lábia). Inclui Intimidação, Sedução e técnicas de Hipnose."),
        AdvantageItem(name = "Medicina", cost = "2 pontos", description = "Permite ao personagem tratar ferimentos (Primeiros Socorros), diagnosticar doenças (Diagnose) e realizar procedimentos avançados, como Cirurgia e Psiquiatria (para problemas mentais)."),
        AdvantageItem(name = "Sobrevivência", cost = "2 pontos", description = "Permite ao personagem lidar com o ambiente selvagem (Alpinismo, Navegação, Meteorologia), caçar, pescar e se esconder (Furtividade) em áreas naturais.")
    )
}
