const fs = require('fs');

const part2 = `MANOBRAS ESPECIAIS
Você desenvolveu manobras de combate únicas, baseadas em um estilo próprio de luta.
Para cada PT você pode escolher três manobras. Manobras não gastam PM. Manobras ofensivas consomem um turno. Manobras defensivas impedem que você utilize manobras ofensivas no próximo turno (exceto se possuir Aceleração) e só podem ser usadas uma vez por turno. Manobras ágeis (de MOV) podem ser usadas livremente, mas apenas uma por turno. Manobras passivas apenas oferecem bônus simples, geralmente em outras manobras. Algumas manobras exigem testes.
Manobras Ofensivas
Ambidestria: Permite atacar com duas armas. Se causar dano, causa +1d de dano extra.
Arremessar Oponente: Se superar a FD do alvo indefeso, pode lançá-lo para LNG (ou uma casa por PT que superar a FD). Ele sofre pelo menos 1d6 de dano de queda e estará caído.
Arremesso Brutal: Pode usar F para lançar objetos como ataque à distância, como se tivesse PDF igual a F/3 (arredondado para baixo). O objeto deve ser adequado para arremesso (como pedras, escombros ou até um inimigo pequeno) e dentro da capacidade do personagem. Objetos arremessados se deterioram ou se tornam inutilizáveis após o uso.
Ataque Arriscado: Aumenta Chance de Crítico e Multiplicador de Crítico em +1, mas ao custo de FA-2.
Ataque Audaz: -2 na FA, mas se causar dano, o alvo perde +1d PV extra.
Ataque de Carga: Usa o corpo no ataque. FA: H+A/2+1d6 (dobrando A em críticos).
Ataque Destruidor: Se superar a FD do alvo e ele falhar ao testar A, o alvo recebe FD-2 por 1d turnos.
Ataque Letal: Recebe Chance de Crítico +1, mas ao custo de FA-1.
Ataque Ousado: Teste H antes do ataque. Se passar, recebe +1d na FA; se falhar, sofre -1d.
Ataque Potente: Concentra-se por um turno; no próximo ataque recebe F ou PDF +1.
Ataque Violento: Concentra-se por um turno; no próximo ataque, Multiplicador de Crítico +1.
Atordoar: Se superar a FD e o alvo falhar no teste de R, ele fica atordoado.
Cegar: Se superar a FD e o alvo falhar no teste de A, ele fica cego por 1d turnos.
Contra-Golpe: Se superar uma FA, recebe FA +2 contra o atacante na próxima ACT.
Debilitar: Se superar a FD do alvo, se ele falhar ao testar A, recebe -1 em um Atributo à escolha do atacante por 1d turnos.
Derrubar: Se superar a FD e o alvo falhar no teste de A, ele cai (levantar custa um MOV).
Desabilitar: Se superar a FD e o alvo falhar no teste de A, desativa uma Vantagem dele por um turno. A estética de como isso ocorre e as Vantagens que podem ser desativadas dependem de interpretação e permissão do mestre.
Desarmar: Se superar a FD, faz o alvo largar sua arma. Recuperá-la custa um MOV.
Desestabilizar: Se superar a FD e o alvo falhar no teste de A, impõe FA-2 ou FD-2 por 1d turnos.
Empurrar: Se superar a FD e o alvo falhar no teste de A, empurra-o uma distância igual à sua F ou PDF (o Atributo que foi utilizado na manobra), caso colida o alvo com algum obstáculo causa dano normal de contusão.
Expor: Se superar a FD, pode infligir FD-2 contra o próximo ataque de um aliado.
Finishing Strike: Caso acerte todas as manobras de uma Sequência, role 1d e tire 1, se tiver sucesso finaliza a sequência com um ataque fulminante com F ou PDF +1d. Essa manobra só pode ser utilizada uma vez por CNA.
Finta: Se superar a FD com uma Manobra Ofensiva, recebe apenas no próximo ataque Chance de Crítico ou Multiplicador de Crítico +1 (desde que seja uma manobra diferente ou um ataque normal).
Flanquear: Se o alvo já foi atacado nesse turno, recebe Chance de Crítico +1 contra ele.
Golpe Circular: Ataca três adversários JNT à sua frente com a mesma FA.
Golpe Crítico: Uma vez por CNA, abra mão de uma ACT para realizar um ataque com F+2.
Golpe Pesado: Usando arma com as duas mãos, recebe Chance de Crítico +1.
Incapacitar: Contra um alvo indefeso, se ele falhar em A e R, fica incapacitado por 1d turnos.
Imobilizar: Se superar a FD e o alvo falhar ao testar H ou A (escolha do alvo), ele fica imobilizado. Pode abrir mão da ACT para mantê-lo preso. O alvo pode tentar escapar em cada turno testando F, H ou A. Se estiver caído, recebe +2 na FA de imobilização.
Impedir: Se superar a FD, impede o MOV do alvo no próximo turno.
Investida: Um ataque que também é um MOV. FA: F+A. Em tabuleiro, cada casa percorrida adiciona FA+1.
Lacerar: Se superar a FD e o alvo falhar no teste de R, sofre 1PV de dano contínuo por 1d turnos (curável com Medicina ou cura mágica).
Postura Ofensiva: FA +1, Chance de Crítico +1, mas FD -1 e sem crítico na FD. Pode ser aprimorada até FA +3 e FD -3.
Quebrar: Se superar a FD, reduz o uso do equipamento do alvo em 1PT ou o desativa por 1d turnos.
Queima-Roupa: Ao atacar com PDF um alvo JNT, recebe Chance de Crítico +1.
Rebater: Caso seja alvo de uma manobra que possua e falhe contra você, apenas no seu próximo turno recebe +2 (FA, FD ou testes) para utilizá-la contra quem te atacou.
Resposta: Se superar uma FA e tiver sucesso ao testar A, causa perda de 1PV no seu atacante com uma Reação.
Saque: Escolha um Tipo de Dano e um Atributo de ataque. Seu primeiro ataque ao sacar sua arma possui FA+2. Pode, após isso, resetar a manobra com uma ACT para reutilizá-la em seu próximo turno.
Sequência: Pode encadear até três manobras sem repeti-las. Cada sucesso exige testar 1d: 1-3 (2ª manobra), 1-2 (3ª). Essa manobra só pode ser utilizada uma vez por CNA.
Tiro Crítico: Uma vez por CNA, abra mão de uma ACT para realizar um ataque com PDF+2.
Manobras Defensivas
Bloqueio: Recebe +1d na FD.
Bloqueio Arriscado: Ignora completamente um ataque, mas fica indefeso pelas próximas duas ACT.
Combo-Breaker: Reduz a chance do atacante executar cada manobra da Manobra Ofensiva: Sequência em -1.
Contra-Técnicas: +1 para resistir a efeitos de manobras (pode ser aprimorada até +3).
Defesa Crítica: Uma vez por sessão, defenda com A+2, mas não age no próximo turno.
Defesa Improvisada: Usa um item do cenário (cadeira, mesa, porta) ou peça de roupa (manto, cachecol, capa) para atrapalhar o atacante. Ganha FD +2 e um inimigo pode gastar uma ACT para destruí-lo.
Defesa Ousada: Teste A antes da FD. Se passar, recebe +1d na FD; se falhar, sofre -1d.
Defesa Plena: Se superar uma FA, pode testar A para ignorar Dano de Impacto.
Defesa Vigorosa: Abre mão da ACT para receber Chance de Crítico e Multiplicador de Crítico +1 na FD até o próximo turno.
Defesas Ágeis: Pode executar um máximo de manobras defensivas equivalente ao seu valor de H+1 até seu próximo turno.
Postura Defensiva: FD +1 e imune a críticos, mas FA -1 e sem ataques críticos. Pode ser aprimorada até FD +3 e FA -3.
Resistência Adaptada: Escolha um Tipo de Dano, você recebe FD+1 contra esse Tipo de Dano. Pode ser recomprado até um máximo de FD+3.
Manobras Ágeis
Avanço: Rola INI novamente e usa o melhor resultado.
Disparada: Abre mão da ACT para se mover três vezes.
Escudo Vivo: Se estiver JNT a um aliado, pode fornecer FD+1 a ele. Essa manobra pode ser recomprada até FD+3.
Postura Reativa: Se superar uma FA, pode imediatamente usar um MOV para reposicionar-se.
Reposicionar: Troca de lugar com um inimigo JNT se ele falhar em disputa de A x H.
Manobras Passivas
Combate Cego: Consegue atacar sem precisar enxergar o oponente e sem sofrer redutores por isso. Não funciona contra Invisibilidade.
Distrair: Abra mão do seu MOV para infligir FA-1 e FD-1 por um turno um única vez no combate.
Estilo de Luta: Você possui um estilo de luta diferenciado. Escolha dois Atributos, uma recebe +1 e outra -1. Essa manobra pode ser adquirida diversas vezes para diversos estilos com bônus diversificados. Você deve possuir ao menos 1PT no Atributo escolhida para o redutor.
Impacto Direcionado: Um personagem pode usar seu PDF para empurrar, deslocar ou derrubar objetos à distância, desde que a natureza do PDF permita (como rajadas, empuxo, manipulação de força, etc.). Ele age como se tivesse F igual a PDF/3 (arredondado para baixo) para essas ACT. Não é possível erguer, carregar ou puxar objetos — apenas movê-los com impacto direto. A aplicação deve ser descrita de forma coerente com a origem do poder. Testes exigidos seguem as regras normais para F.
Manobras Ágeis Pode testar H para reduzir o tempo de uso de uma manobra em um grau: De um turno para uma ACT ou de um ACT para um MOV. Não poderá usar outras manobras nesse turno (mas ainda pode agir normalmente).
Manobras Avançadas: Ao utilizar uma manobra recebe +1 em testes para executá-la. Essa manobra pode ser comprada várias vezes aumentando o bônus para até +3.
Reação Rápida: Permite realizar manobras ofensivas no seu próximo turno, mesmo após ter utilizado manobras defensivas no turno anterior.
Reprise: Caso utilize a mesma manobra num turno seguinte, recebe +1 para executá-la (não cumulativo).
MEMBROS EXTRAS
Você possui múltiplos braços, pernas, caudas ou tentáculos, mas os PT gastos na Vantagem não refletem necessariamente a quantidade exata de membros.
1PT: Você ganha +1 em FA, FD ou em testes físicos, podendo escolher o bônus antes de cada ação ou deixá-lo pré-definido com o mestre. Também é capaz de usar um EQP adicional. Pode adquirir Monstruoso e/ou Modelo Especial, mesmo excedendo o limite de Desvantagens. Se desejar, pode abrir mão de todos os efeitos da Vantagem para realizar uma ação extra de manipulação ou interação no seu turno, por cada ponto gasto nessa Vantagem. (1PM) Aumenta o bônus para +2 em um MOV.
2PT O bônus base de Membros Extra aumenta para +2, e você pode usar dois EQP adicionais. (2PM) Eleva o bônus para +3 em um MOV.
3PT O bônus aumenta para +3, e você pode usar até três EQP adicionais. (3PM) Você pode trocar o bônus da Vantagem por 1d bônus, sem consumir ACT.
Observação: O mestre pode decidir que um personagem com Membros Extra seja considerado Monstruoso e/ou Modelo Especial, dependendo do cenário e da anatomia do personagem.
MEMÓRIA EXPANDIDA
Você tem memória infalível. Lembra tudo ligado aos cinco sentidos, nunca esquece nada.
1PT Ao ver outro personagem usar uma PRC, pode aprendê-la e usá-la como se a tivesse. Pode copiar apenas uma PRC – para copiar uma nova PRC primeiro é preciso “apagar” a anterior. PRC copiadas são utilizadas como PRC de 1PT.

2PT Pode copiar uma PRC extra de 1PT ou uma única PRC de 2PT. (1PM/PT) Pode copiar valores de Atributos de oponentes observando o alvo em combate por um turno. Cada PT copiado custa 1PM e dura uma CNA. Exemplo: Copiar uma F5 custa 5PM.

3PT Pode copiar até 3PT em PRC. (2PM/PT) Pode copiar Vantagens de outros personagens ao vê-la em execução por um turno. Pode-se copiar até 3PT de Vantagem. Após um PRD todas as Vantagens são esquecidas.
MENTOR
Alguém lhe ensinou suas técnicas de combate, poderes ou conhecimentos. Você pode ainda ter contato com essa pessoa ou apenas recordar seus ensinamentos.
1PT Escolha uma PSV ou ATV de uma Vantagem, ou duas Manobras Especiais que possua. Você recebe um bônus permanente de +2 ao utilizá-las. (1PM) Você pode recordar um ensinamento que forneça auxílio para resolver um problema (ou um bônus de +2 em um teste).
2PT Escolha uma nova PSV ou ATV de uma Vantagem, ou duas novas Manobras Especiais que possua. Recebe um bônus permanente de 1d ao utilizá-las. (2PM) Pode repetir um teste falho, uma vez por teste.
3PT Uma vez por sessão, pode optar por obter sucesso automático em qualquer teste (em combate, conta como crítico). (3PM) Pode relembrar um ensinamento profundo de seu mentor e ganhar 1d bônus em qualquer teste.
PARCEIRO
Você e um aliado possuem um vínculo de batalha único, lutando em sincronia perfeita.
1PT: Quando lutam em dupla e ambos possuem esta Vantagem, agem como se fossem um único lutador. Realizam apenas uma ACT e um MOV por turno, usando os melhores valores de Atributos entre vocês. Compartilham Vantagens enquanto lutam juntos. Qualquer dano recebido é dividido igualmente entre os dois (arredondado para cima). (1PM) Caso não esteja em dupla, pode abdicar de sua ACT para conceder +1 em FA ou FD a um aliado JNT.
2PT: Se um aliado atacar um alvo (mesmo sem causar dano), você ganha +2 na FA ao atacar o mesmo alvo na sua próxima ACT. (2PM) Quando dois personagens com Parceiro de 2PT lutam juntos, podem realizar uma ACT com 1d bônus em um turno. Ataque e defesa contam como ACTs separadas. (2PM) Caso não esteja em dupla, pode abdicar de sua ACT para conceder +3 em FA ou FD a um aliado JNT.
3PT: Dois personagens com Parceiro de 3PT podem rolar dois dados para o mesmo teste e escolher o melhor resultado, desde que estejam JNT. (3PM) Enquanto não luta em dupla, se um aliado JNT acertar um crítico, você pode desferir um crítico no mesmo alvo em sua próxima ACT. (3PM) Enquanto não luta em dupla, pode conceder 1d bônus em FA e FD a um aliado JNT por um turno.
PODER ESPECIAL
Possui algum domínio e compreensão do principal poder do cenário de campanha, pode ser magia convencional, ki, nen, soma, hamon, cosmo ou qualquer outra coisa.
Um mesmo cenário de campanha pode ter mais de um tipo de poder, como em Final Fantasy que existe a magia comum, mas também a azul (baseada em habilidade de monstros). O modo como tais poderes se relacionam e afetam outros elementos do jogo são definidos pelo cenário e administrados pelo mestre.
Em alguns cenários onde um personagem use PDR, tenha EQP místicos (que utilizam o PDR do cenário) ou receba os benefícios de PDR de um aliado ou por outro motivo, ganha uma escala. O mestre decide quando aplicar.
Algumas das habilidades de PDR podem não condizer com cenários de baixo misticismo ou realistas. Cabe ao mestre adaptar ou eliminar efeitos para manter a sessão agradável.
PDR naturalmente utiliza palavras e gestos para sua utilização, mas isso também pode ser readaptado conforme o cenário.
1PT Recebe +3 em testes de PRC relacionados ao poder. Pode mudar o Tipo de Dano de seus ataques para o do PDR, livremente. Pode acrescentar o PDR ao Tipo de Dano de um aliado JNT por um turno, abrindo mão de seu MOV. (1PM) Consegue sentir fontes de PDR PRT. (1PM) Pode transferir 1PM para um personagem JNT em um MOV. (1PM) Pode aumentar o bônus de uma Vantagem em +1 usando seu MOV. (XPM) Cancela efeitos de PDR ou Vantagens que consiga detectar e identificar (com exceção de Status Negativos e Mácula que só podem ser canceladas por Restauração). O custo em PM equivale ao dobro do custo em PM do PDR ou Vantagens que pretende cancelar. Caso não identifique o efeito, pode tentar “chutar” um custo aleatório de PM, mas correndo o risco de perder PM sem sucesso. Se o usuário original de um PDR ou Vantagem tenha R maior que a sua, você não poderá cancelá-la. Também não se pode cancelar PDR de nível maior que o seu.
2PT A transferência de PM agora pode ser usada à distância (PRT), também pode transferir 2PM por MOV. Pode acrescentar PDR ao Tipo de Dano de um personagem PRT, em um MOV. Pode cancelar PDR ou Vantagens de personagens com R superior. (2PM) Pode aumentar o bônus de uma Vantagem em +2, usando seu MOV. (2PM) Pode aumentar em um grau o efeito de uma Vantagem (como alcance ou duração). Por exemplo, pode fazer com que aceleração afete você e um aliado JNT, ou aumentar o alcance de Toque de Energia para PRT (aliados inclusos), em seu MOV.
3PT Pode transferir até 3PM por MOV. Pode acrescentar o PDR ao Tipo de Dano de um aliado PRT por uma CNA, em um MOV. (3PM) Após realizar um ataque em que a FA tenha superado a FD do alvo, pode utilizar um PDR em seguida contra o alvo como uma ACT extra. (3PM) Prepara um PDR antecipado, abrindo mão de seu turno e suas Reações, assim que quiser poderá utilizar seu poder como uma Reação. Só se pode preparar um PDR por vez e dura um número de turnos igual sua R, caso não o use dentro desse tempo o PDR se dissipa; (3PM) Pode aumentar o bônus de uma Vantagem em +3, em um MOV. (3PM) Pode aumentar em dois graus o efeito de uma Vantagem, em um MOV.
TÉCNICAS
	Ao adquirir PDR o personagem adquire três Técnicas (TEC), que podem ser magias, golpes especiais ou similares. Uma TEC é nada mais nada menos que uma versão mística de uma Vantagem. Assim se um usuário de PDR escolhe Aceleração, Regeneração e Cura como suas TEC, ele poderá usar essas Vantagens como se as tivesse, mas sob as seguintes condições:
Cada PT de Vantagem é considerado como uma TEC separada. Assim, Aceleração(3) equivale a três TEC distintas, sendo que não se pode aprender TEC de Vantagens de PT elevado sem antes ter aprendido as de PT menor. Então um usuário de PDR não poderia aprender a TEC de Aceleração(2) sem antes aprender a TEC de Aceleração(1);
O personagem precisa gastar um custo em PM equivalente ao PT da TEC para primeiro “ativá-la” (o que toma um MOV) e depois utilizar a Vantagem como se a possuísse. Assim, para usufruir dos efeitos de Aceleração(3), primeiro é preciso gastar 3PM para ativar a técnica (o que dura uma CNA) e depois poder, por exemplo, cortar os PM necessários para usar um efeito de Aceleração.
TEC não recebem benefícios da Vantagem PDR (como aumento de bônus ou efeito).
Um personagem não pode aprender como TEC uma Vantagem de PT maior que o PT que possuir em PDR. Assim, para ter a TEC de Aceleração(3) é preciso antes possuir PDR(3).
Para aprender uma TEC o personagem precisa estudar e praticar (o que pode tomar alguns dias de estudo) ou ver outro personagem utilizando a TEC ou a própria Vantagem e passar num teste de H.
PODER OCULTO
Você possui um poder latente que emerge em momentos críticos, elevando suas capacidades além do normal.
1PT (1PM) Pode abdicar de seu turno para se concentrar, recebendo +2 em FA e testes físicos ou +2 em FD e testes de R por uma CNA. O bônus pode ser ajustado no meio do combate usando uma ACT. Bônus máximo de +4.
2PT Bônus máximo aumenta para +6. (2PM) Em um turno de concentração, pode adicionar +3 em FA e testes físicos ou +3 em FD e testes de R. (2PM) Em um turno de concentração, pode receber 1d bônus em FA e testes físicos ou FD e testes de R.
3PT Bônus máximo aumenta para +8. Pode alcançar até 2d bônus em vez de 1d. (3PM) Em um turno de concentração, pode adicionar +4 em FA e testes físicos ou +4 em FD e testes de R.
POSSESSÃO
Você é capaz de tomar controle do corpo de outra criatura, explorando suas capacidades físicas.
1PT (1PM/CNA) A vítima deve estar dormindo, inconsciente ou indefesa e JNT. Enquanto em posse do corpo, você utiliza todos os Atributos, Vantagens e Desvantagens físicas do hospedeiro. Conhecimentos e valores morais do alvo, como PRC ou certas Desvantagens permanecem inalterados. A vítima tem direito a testar R para anular o efeito. Após a possessão, a vítima pode testar R para perceber que foi possuída, mas não saberá o que foi feito durante o controle. Você não pode possuir uma criatura com R superior à sua. Só se pode possuir um alvo por vez.
2PT (+2PM) Pode tentar possuir alvos com R superior à sua. (+2PM) Pode impedir que a vítima lembre da possessão. (+2PM/CNA) Pode possuir vítimas conscientes, mas elas recebem +2 no teste de R. Vítimas conscientes podem refazer o teste de R sempre que receberem dano, ou alguém tentar despertá-las emocionalmente (ex.: "Resista! Não deixe que ele controle você!").
3PT (1PM/Memória) Pode acessar qualquer memória da vítima durante a possessão. Pode visualizar e utilizar a ficha do alvo, incluindo suas PRC. Caso o alvo tenha PDR, o possessor só conseguirá utilizá-lo se também o tiver, e do mesmo tipo. (+3PM/CNA) Consegue possuir alvos PRT. Para isso, deve estabelecer contato visual. Sem contato visual, a vítima recebe +2 no teste de R. A possessão termina se a vítima sair de PRT.
PREMONIÇÃO
Você tem a capacidade de prever eventos futuros ou deduzir informações importantes com precisão extraordinária, seja por habilidades sobrenaturais ou um poder de dedução aguçado.
1PT Em momentos específicos, determinados pelo mestre, você recebe uma previsão do futuro. Você também pode se concentrar calmamente para tentar prever algo, embora as informações possam ser vagas. (1PM) Ao analisar um objeto, pode obter informações do mesmo (quem o criou, quem o usou por último, onde esteve, para que foi ou será usado, etc.). (1PM) Durante um MOV, pode prever instantes futuros, recebendo +1 em testes no turno, incluindo testes de combate (FA e FD).
2PT Caso possa ver claramente um personagem, pode usar um MOV para prever sua expectativa de vida (quantos PV tem). (2PM) Durante um MOV, pode prever eventos próximos, recebendo +2 em testes no turno, incluindo testes de combate (FA e FD).
3PT Uma vez por sessão, pode "voltar no tempo" em até uma CNA (fora de combate) ou um turno (em combate), como se tudo não passasse de uma visão ou dedução sua. Pode meditar por dois turnos para fazer uma previsão detalhada de um personagem, fornecendo 1d bônus a uma rolagem qualquer a escolha dele. Não pode fazer outra previsão até que a anterior se cumpra (ou seja, que o personagem use o 1d bônus). (3PM) Pode analisar e receber claramente a informação precisa sobre uma pessoa ou lugar. (3PM) Ao se concentrar por um turno, descobre o que está acontecendo com um personagem, independente de distância.
PM EXTRA
Você possui uma reserva extra de PM.
1PT Recebe R+2, mas apenas para calcular seu total de PM. Em outras palavras, você recebe 10PM além de seu limite.
2PT Recebe R+4, mas apenas para calcular seu total de PM. Em outras palavras, você recebe 20PM além de seu limite.
3PT Recebe R+6, mas apenas para calcular seu total de PM. Em outras palavras, você recebe 30PM além de seu limite.
PV EXTRA
Você possui uma reserva extra de PV.
1PT Recebe R+2, mas apenas para calcular seu total de PV. Em outras palavras, você recebe 10PV além de seu limite.
2PT Recebe R+4, mas apenas para calcular seu total de PV. Em outras palavras, você recebe 20PV além de seu limite.
3PT Recebe R+6, mas apenas para calcular seu total de PV. Em outras palavras, você recebe 30PV além de seu limite.
QUALIDADES ESPECIAIS
Você possui características únicas que o diferenciam dos outros, seja por herança genética, treinamento intenso, mutações ou mesmo eventos extraordinários.
Por 1PT escolha três qualidades abaixo:
Aderência: Escala superfícies íngremes ou lisas (inclusive paredes de vidro) sem dificuldade, em velocidade normal e sem testes. Recebe +2 contra desarmes ao segurar itens ou equipamentos.
Ágil: É imune à Lentidão causada por efeitos comuns ou de PRC, recebe R+3 contra Lentidão causada por PDR ou Vantagens. Também recebe H+1 para testes de corrida e definir velocidade.
Anaeróbico: Não precisa respirar, tornando-se imune a efeitos de asfixia e toxinas aéreas.
Andar na Água: Caminha sobre líquidos como se estivesse em terra firme.
Centrado: É imune à Atordoamento causado por efeitos comuns ou de PRC, recebe R+3 contra Atordoamento causado por PDR ou Vantagens e contra perda de concentração.
Constância: Move-se normalmente por terrenos difíceis ou obstáculos que reduzem MOV, sendo imune a penalidades de movimento (mas não a paralisia).
Controle Digestivo: Possui total controle de suas funções digestivas. Possui R+2 contra venenos. Pode utilizar seu estômago para armazenar pequenos itens (engolindo e regurgitando como desejar). A quantidade e tamanho de itens é proporcional ao tamanho do personagem e deve ser combinada com o mestre.
Corpo de Ferro: Consegue +3 em uma FD e um sucesso automático em um teste de A por sessão, além de +1 em testes de A.
Clareza: É imune à Confusão causada por efeitos comuns ou de PRC, recebe R+3 contra Confusão causada por PDR ou Vantagens. Possui R+1 contra efeitos mentais.
Coração de Pedra: É imune à Afeição causada por efeitos comuns ou de PRC, recebe R+3 contra Afeição causada por PDR ou Vantagens. Por 1PM pode fornecer os mesmos bônus para um aliado JNT, com um MOV.
Criptobiose: Controla o metabolismo, podendo, por 1PM, pausá-lo completamente para ignorar STN ou estabilizar-se em Teste de Morte, impedindo que piore com o tempo, mas ficando indefeso e inconsciente até determinar que deseja acordar.
Deslizar: Move-se deslizando no chão em velocidade de corrida sem se cansar, como se estivesse caminhando. Recebe +2 em MOV e um sucesso automático em um teste de MOV e uma esquiva por sessão.
Destemido: É imune à Medo causado por efeitos comuns ou de PRC, recebe R+3 contra Medo causado por PDR ou Vantagens. Seu medo nunca se torna pavor.
Destruidor: Ao utilizar a Manobra Especial: Desarme para danificar um objeto ou EQP, dobra sua F ou PDF na FA.
Equilíbrio: Nunca cai. Se for arremessado, aterrissa de pé, ignora 1d de dano por queda e não gasta MOV para se levantar. Não é afetado por superfícies escorregadias.
Erro Prudente: Após uma falha, teste R, se passar recupere 1PV ou 1PM (à sua escolha).
Escavação: Move-se através de solo sólido na velocidade normal, criando um túnel que pode deixar aberto ou colapsar.
Estômago de Avestruz: Consegue comer qualquer coisa sem problemas, até comidas estragadas ou repugnantes. Possui R+2 contra doenças, sendo imune a envenenamento por comida ou intoxicação alimentar.
Flexível: Contorcionista ou de corpo maleável, passa por lugares estreitos sem testes e recebe 1d bônus para escapar de agarrões.
Foco: Não sofre penalidades ambientais em testes que exigem concentração. Pode gastar 1PM para não perder a concentração ao receber dano.
Força Extra: Consegue FA+3 em um ataque baseado em F e um sucesso automático em um teste de F por sessão, além de +1 em testes de F.
Glutão: Recupera 5PV e PM por refeição (em vez de apenas 2) e uma vez por CNA pode se alimentar para recuperar 2PV e PM. Caso tenha muita comida disponível (100 moedas em comida fresca), pode se alimentar por uma CNA inteira para recuperar todos os PV e PM, mas recebe um redutor de -1d por ficar estufado por uma CNA.
Impulsividade: Recebe INI +3 e uma vez por sessão, é o primeiro a agir no combate sem rolar  INI.
Longevidade: Você não envelhece, sendo imune a efeitos de envelhecimento ou rejuvenescimento.
Mira: Recebe FA+3 em um ataque baseado em PDF e um sucesso automático em um teste de PDF por sessão, além de +1 em testes de PDF.
Nado Anfíbio: Nado em velocidade máxima, como se fosse em terra firme.
Perseverança: Pode gastar 1PM para cancelar temporariamente um redutor de -1 por uma CNA.
Quase Voo: Movimenta-se no ar usando cordas, teias ou grandes saltos. Gastar ACT é necessário para estabilizar-se; cairá se não puder usá-la.
Queda Segura: Pode cair de qualquer altura sem se ferir, desacelerando a queda com saliências ou superfícies.
Resiliência: Permanece consciente mesmo estando Inconsciente, Muito Ferido ou Quase Morto, mas sem poder agir.
Robustez: É imune à Debilitação causada por efeitos comuns ou de PRC, recebe R+3 contra Debilitação causada por PDR ou Vantagens. Também possui R+1 contra Dano Contínuo.
Sabe Tudo: Um sucesso automático por sessão em testes de conhecimento geral. Pode gastar 1PM para relembrar detalhes específicos.
Sem Dor: Resistente à dor, recebe FD+1 e +2 em testes contra dor (tortura, etc.).
Sem Fome: Só precisa de água para viver. Talvez tenha treinado seu corpo e mente para ignorar efeitos de inanição, talvez coma até pedra sem problema ou talvez seja um alien.
Sem Rastros: Não deixa pegadas — mesmo sobre a neve ou areia. Você ainda pode ser rastreado por criaturas com sentidos especiais.
Sem Sede: Não sofre efeitos de inanição por ficar sem água. Talvez tenha vivido em desertos ou talvez seja um mutante.
Sem Sono: Não precisa dormir, mas ainda pode descansar, caso queira.
Sono Leve: Dorme de forma superficial, podendo rolar testes como se estivesse acordado. Recebe +3 contra efeitos de Sono.
Sortudo: Uma vez ao dia pode rolar novamente um teste (a segunda rolagem substitui a primeira) ou uma rolagem (positiva) na tabela de “Aleatoriedades” que se encontra no capítulo “Miscelânea”.
Viagem Espacial: Capaz de viajar rapidamente no vácuo e imune aos seus efeitos.
RECURSOS
	Você tem acesso a uma fonte de apoio excepcional, seja ela oriunda de um patrono poderoso, riqueza, influência ou status elevado. No entanto, esses recursos não são infinitos e devem ser usados com sabedoria.
1PT Pode invocar seus recursos uma vez por sessão sem custo em PM. (1PM) A forma da ajuda é determinada pelo Mestre, podendo incluir desde suprimentos básicos até uma chance de influenciar um evento importante. Subornos: Uma oportunidade de negociar com um NPC hostil. Apoio: Contratar mercenários ou aliados temporários. EQPs ou Consumíveis: Fornecimento de itens úteis para a missão. A ajuda invocada dura uma Cena (CNA) e desaparece após o uso, seja quebrando, evaporando ou finalizando contratos. Se o recurso invocado representar uma Vantagem, o valor máximo é de 1PT.
2PT Você pode ser um policial ou agente do governo, com todos os poderes legais que isso acarreta. Pode portar armas, acessar informações confidenciais, invadir locais restritos e prender ou eliminar pessoas sem grandes repercussões. (2PM) Antes de sair para uma missão, pode adquirir um Item Consumível (geralmente de cura) que dura enquanto a missão durar. (2PM) Possui à disposição itens essenciais para suas missões (uma mochila ou bolsa de parafernálias). Recebe ou pode fornecer a um aliado +2 em testes de PRC. (2PM) Seu recurso pode representar até 2PT de Vantagens.
3PT Você é uma pessoa de prestígio, como um diplomata, príncipe ou embaixador, fora da maior parte dos sistemas legais. (3PM) Antes de iniciar uma missão, pode adquirir um item ou equipamento útil relacionado ao contexto da missão (como uma arma específica contra monstros ou ferramentas para infiltração). (3PM) Seu recurso pode representar até 3PT de Vantagens.
REGENERAÇÃO
Você possui um fator de cura excepcional, tornando-se extremamente difícil de eliminar. Seu corpo se recupera rapidamente de ferimentos graves e até mesmo da morte, em algumas circunstâncias.
1PT Recupera sua R em PV por CNA. Seu descanso é sempre pleno.
2PT Regenera 1PV cada dois turnos em combate (use um dado e escolha par ou ímpar para marcar o turno em que você regenera, ou uma moeda e escolha um dos lados), a regeneração começa no seu segundo turno de combate. Recupera Rx2PV por CNA. Qualquer descanso recupera todo seu PV.
3PT Regenera 1PV por turno em combate e Rx3PV por CNA.
RESISTÊNCIA ESPECIAL
Você possui uma resistência superior contra um tipo específico de ataque ou poder, tornando-se mais difícil de ser afetado por ele.
1PT Recebe +3 numa categoria de ataque que exija R. Escolha uma categoria: 
STN: Inclui todos os efeitos de status.
PDR: Escolha um tipo específico, como Magia ou Ki.
Vantagens: Abrange todas as vantagens que exijam testes para ressitir.
2PT Recebe 1d bônus ao testar R contra o tipo de ataque escolhido. (2PM) Escolha um ataque específico dentro da categoria de sua resistência (como uma única Vantagem, ou um único STN), você pode ignorar completamente o efeito.
RESTAURAÇÃO
Você pode curar uma ou diversas condições que põem a vida do alvo em risco.
Ao comprar Restauração escolha um dos seguintes STN: Afeição, Asfixiar, Atordoar, Confusão, Dano Contínuo, Debilitar, Desnutrição, Lentidão, Paralisia, Medo ou Sono. Cada STN custa 1PT para compra, 1PM para cura e toma uma ACT.
Você pode optar por curar Máculas ao invés de STNs. O personagem pode curar efeitos de Mácula com custo total em PT igual ou inferior ao valor investido em Restauração de Mácula. Cada efeito curado custa 1PM por PT da Mácula removida, e toma uma ACT.
Em uma única ACT, você pode curar até 3 efeitos simultaneamente, sejam STN ou máculas ao custo de +1PM por efeito adicional curado.
Você pode curar alvos adicionais ao mesmo tempo, gastando 1PM extra por alvo além do primeiro.
SENTIDOS ESPECIAIS
Seus sentidos são mais aguçados que o normal. Quando possuem alcance, funcionam automaticamente em PRT, mas exigem testes em LNG. Sempre que possível, um sentido indica a direção e distância aproximada do alvo detectado, respeitando suas limitações naturais.
Se exposto a uma fonte intensa do próprio sentido — como um som extremamente agudo para Audição Aguçada ou uma grande concentração de energia para Detectar Energia — o usuário fica atordoado enquanto estiver PRT e por 1 turno após se afastar.
Cada 1PT concede três sentidos à sua escolha.
Audição Aguçada: Capta sons baixos, distantes ou em frequências inaudíveis para humanos.
Detectar Criatura: Escolha uma espécie de ser vivo. Você sente sua presença quando está próxima.
Detectar Energia: Percebe um tipo específico de energia de PDR (ex.: ki, cosmo, nen).
Detectar Mentiras: Ao custo de 1PM por frase, percebe se alguém está mentindo.
Detectar Objeto: Escolha um tipo de objeto (ex.: armas) ou um material (ex.: ouro). Você pode detectar sua presença.
Infravisão: Enxerga calor e encontra facilmente criaturas de sangue quente no escuro.
Olfato Aguçado: Fareja como um cão perdigueiro.
Perceber Emoções: Identifica a emoção de uma criatura PRT ao se concentrar por um MOV.
Radar: Emite ondas (sonoras ou de rádio) e percebe formas e objetos ao seu redor, sem distinguir cores. Funciona mesmo de olhos fechados.
Rádio: "Ouve" frequências de rádio (AM, FM, celular, internet) e pode ser capaz de transmiti-las.
Senso de Direção: Sempre sabe onde fica o norte e consegue retornar por qualquer caminho percorrido, mesmo em labirintos. Facilita encontrar saídas.
Senso do Perigo: Prevê ameaças iminentes e nunca é pego de surpresa.
Sentido Sísmico: Detecta qualquer coisa em movimento em contato com o solo, desde que você também esteja tocando a mesma superfície.
Sentir Índole: Ao custo de 1PM, percebe se uma criatura possui boas, más ou neutras intenções na CNA atual.
Ver o Invisível: Enxerga seres e objetos invisíveis. Invisibilidade e efeitos similares não funcionam contra você. Radar, Infravisão e outros sentidos não detectam invisibilidade.
Visão Aguçada: Enxerga detalhes minuciosos à distância, como uma águia.
Visão no Escuro: Enxerga perfeitamente na escuridão total, mas apenas em tons de cinza.
Visão Microscópica: Consegue ver estruturas minúsculas, como células, grãos de poeira e até mesmo moléculas.
Visão na Penumbra: Enxerga claramente em pouca luz e perfeitamente em noites de lua cheia. Em escuridão total, vê tão mal quanto qualquer outro.
Visão de Raios-X: Consegue ver através de objetos e paredes, exceto aquelas feitas de chumbo, materiais densos ou barreiras mágicas.
SEPARAÇÃO
Você pode criar cópias de si mesmo.
1PT (1PM) Cria uma cópia imperfeita com -1 em todos os Atributos, acumulativo para cada cópia ativa (-2 para duas, -3 para três, etc.). Pode criar uma cópia por turno, com limite máximo de R cópias. Cada cópia nasce com seus PV e PM atuais, mas não pode criar outras cópias. Visualmente, são idênticas ao original e indistinguíveis. Se qualquer versão sua ficar Perto da Morte, atingir 0 PV ou morrer, todas desaparecem.
2PT Cópias têm apenas -1 em Atributos, sem efeito cumulativo. Se uma cópia ficar Perto da Morte, apenas ela desaparece. Se você ficar Perto da Morte, todas desaparecem. Pode criar várias cópias em um único turno. O limite máximo de cópias aumenta para R+2. (2PM) Pode remover o redutor de -1 em Atributos de uma cópia.
3PT Você não cria cópias, mas se divide em múltiplas versões de si mesmo. Se um de vocês ficar Perto da Morte ou morrer, os demais continuam existindo. Cada cópia pode criar novas cópias. Não há limite de criação de cópias, apenas restrição de PM. Nenhuma versão é mais "verdadeira" que as outras; se ao menos uma sobreviver, você permanece vivo. Só pode agir por meio de uma cópia por vez. Para trocar qual cópia está sob seu controle direto, gaste 1 MOV. Se houver mais de 2 cópias em campo, a troca exige um turno. (3PM) Pode criar cópias sem redutor de Atributo. (1PM) Pode absorver uma cópia, recuperando até 5PV. Pode absorver até 6 cópias por turno.
SIMBIONTE
O personagem possui vínculo biológico com um ser de forma alienígena que depende de um hospedeiro para sobreviver e manifestar poderes.

1PT O simbionte concede +2PT fixos em Atributos (máx. +2 em um único Atributo), como PRC ou como Vantagens, além de impor duas Desvantagens obrigatórias (Insanidade ou Código e Fraqueza ou Vulnerável). O simbionte compartilha essas Desvantagens com o hospedeiro, que não deve possuir as mesmas Desvantagens do simbionte. Sem hospedeiro, o simbionte não pode usar Vantagens e age com Atributos 0. Enquanto estiver no hospedeiro, o simbionte é imune a ataques. No entanto, um personagem pode removê-lo ao gastar um turno e passar em um teste de F. O simbionte fica desabilitado até que o hospedeiro se reúna com ele. Sem o simbionte, o personagem sofre -1 cumulativo em todos os testes por CNA, até um máximo de -3. (1PM) Em uma ACT, recebe +1 em um teste de combate, de Vantagens ou PRC fornecido pelo simbionte.
2PT O simbionte passa a conceder no total +4PT fixos. (2PM) Em uma ACT, recebe +3 em um teste de combate, Vantagens ou PRC fornecido pelo simbionte.
3PT O simbionte passa a conceder +6PT fixos no total e permite reduzir o custo de um poder em -1PM por MOV. (3PM) Em uma ACT, recebe +1d em um teste de combate, Vantagens ou PRC fornecido pelo simbionte.
STATUS NEGATIVOS
Permite causar condições debilitantes.
Cada STN custa 1PT, usa 2PM, consome uma ACT e dura uma CNA (se possuir efeito contínuo). Ao aplicá-lo role 1d6: com 1-3 o alvo é afetado.
Alvo sofre o efeito sem resistência no primeiro turno. Em turnos seguintes, pode testar o Atributo do STN para remover o efeito. Se falhar em aplicar o STN, pode tentar outro STN que possua pagando +1PM (máximo de 3 STN).
Redutores: STN impõem -1 a testes, incluindo FA, FD e Atributos. Se o redutor for em R, também reduz PV e PM. STNs diferentes acumulam; iguais, usa-se o maior.
Personagens Artificiais: Personagens com Corpo Artificial são imunes a asfixia, doenças, desnutrição, sono e venenos. Personagens com Mente Artificial são imunes a afeição e medo.
+1PT 1-5 para o STN afetar o alvo.
+1PT (+5PM) Pode-se ignorar um teste de Atributo do alvo. Esse PM deve ser gasto no uso do STN, não no momento do alvo tentar resistir.
Afeição (H): Alvo age como aliado de um personagem. Redutor em ações que não envolvam o “amado”. Pode resistir apenas se confrontado ou acordado por terceiros.
Asfixia (R): Um personagem consegue ficar R+1 turnos sem respirar. Você força o sistema respiratório do alvo a asfixiar. No primeiro turno sem respirar o alvo testa para não perder o MOV. Cada turno seguinte testa-se para que o alvo não perca 5PV temporários. Os PV perdidos são recuperados na mesma proporção caso recupere o fôlego ou seja reanimado com Medicina, caso caia inconsciente.
Atordoar (A): Não pode realizar MOV. Precisa testar A para agir normalmente ou usar um turno para remover o efeito.
Confusão (R): Deve rolar aleatoriamente o alvo de suas ACT. O mestre pode intervir nas ACT do personagem.
Dano Contínuo (A): Um ferimento aberto, envenenamento ou agente corrosivo. Alvo perde 1PV por turno (ou 5PV no fim da CNA, aquele que for maior). Pode testar A por CNA para superar o STN. Dano Contínuo não cessa sem medicação ou teste.
Desnutrição (R): Um personagem consegue ficar R+1 dias sem comer. Você força o corpo do alvo a desnutrir instantaneamente com redutor e perda de 5PV temporários. Seus redutores se acumulam, mas exigem testes de R por acúmulo.
Paralisia (H): Alvo fica paralisado, perdendo seu próximo turno. Cada turno seguinte pode testar H para remover o efeito.
Lentidão (H): Pode realizar apenas ACT. Não permite Vantagens que forneçam múltiplas ações ou usem H como base de bônus extra em FA ou FD. Aceleração anula lentidão. Redutores são cumulativos, permitindo testar H por turno para remover um redutor de -1.
Medo (R): Inflige redutor enquanto alvo ver ou sentir sua presença. Redutor pode acumular, se o redutor zerar a R, deve testar para não ficar apavorado.
	Alvo apavorado deve testar a cada turno seguinte para não fugir. Se ficar FDA, o pavor cessa e torna a ser apenas medo.
	Se encurralado não age ofensivamente, testa por turno ou desmaia. Após cinco turnos encurralado, o personagem testa R: falha gera Insanidade ou Complexo.
Sono (R): Personagens ficam até R+3 PRD sem sentir os efeitos de sono. Você aplica instantaneamente o efeito de sono num alvo.
	Alvos testam para se manter acordados e perderem 5PV temporários, se falharem, caem no sono. Dormindo, testam para acordar se ouvirem qualquer barulho ou podem ser acordados por um aliado (um MOV).
	Redutores podem acumular. Se R zerar com redutores, o personagem cai em sono profundo, só desperta com dano.
	Personagens recuperam 5PV temporários por CNA de descanso num sono normal. Com sono induzido, recupera-se todos os PV temporários após uma CNA de descanso.
TELEPATIA
Você é capaz de ler e influenciar pensamentos com o poder da mente.
1PT Pode se comunicar telepaticamente com qualquer criatura dentro de seu campo de visão. Pode visualizar os sonhos de um alvo adormecido. A telepatia só funciona com criaturas vivas e que você consiga ver. Não afeta alvos com R superior à sua. Se o alvo não for humano, não possuir linguagem ou for Inculto, você apenas compreende seus sentimentos e pode acessar suas memórias. Qualquer alvo de sua telepatia pode perceber uma presença estranha em sua mente ao passar em um teste de H. (1PM) Descobre uma Vantagem ou Desvantagens do alvo. (1PM) Recebe H+2 para testes sociais. (1PM) Antecipando os próximos de um único alvo, recebe INI+1 e FD+1 contra ele até o fim do combate. (1PM) Descobre se um alvo carrega itens valiosos. 
2PT (+1PM) Ao prever as ações de um alvo o bônus aumenta para +2. (2PM) Localiza um personagem sem necessidade de testes, caso esteja PRT. Além de PRT, pode localizá-los ao passar em um teste de H. Depois de localizar um alvo, pode iniciar uma conversa mental. (2PM) Gastando um turno, pode escanear completamente um alvo e visualizar sua ficha. (2PM) Emite uma onda psíquica contra a mente do alvo, com PDF igual ao seu valor de H. O ataque ignora H na FD, mas causa apenas H + 1d de dano.
3PT (3PM) Pode afetar alvos com R superior à sua, desde que estejam indefesos (adormecidos ou imobilizados). Deve se concentrar por um turno e realizar uma disputa de R x H. Se o alvo resistir por três turnos seguidos, sofre uma insanidade definida pelo mestre e passa a considerar o telepata um inimigo eterno. (3PM) Pode impor uma emoção ao alvo (amor, calma, coragem, desespero, medo, ódio, etc.), determinando também seu foco (o que o alvo amará, odiará, temerá, etc.). O efeito é definido pelo mestre, concedendo bônus ou penalidades apropriadas. O alvo pode testar R x H para resistir sempre que for afetado ou quando sua emoção for desafiada. Emoções opostas podem se anular: Amor cancela Ódio (e vice-versa); Calma cancela Desespero (e vice-versa); Coragem cancela Medo (e vice-versa). (X PM) Pode alterar as memórias e personalidade do alvo, ou até mesmo apagar sua mente, deixando-o em estado vegetativo. Cada modificação requer um turno e consome PM: Alterar memórias: 2PM por mudança; Modificar traços de personalidade: 10PM por traço; Apagar completamente a mente: 20PM. O alvo pode testar R x H para resistir, e recebe novas chances ao vivenciar eventos ou diálogos que contradigam as memórias implantadas. (3PM) Pode usar seu poder mental para conceder +1d de bônus na FA ou FD para si ou um aliado PRT.
TELEPORTE
Você desaparece de um lugar e reaparece em outro.
1PT Pode se teleportar para locais visíveis sem teste, carregando apenas seus pertences pessoais. O alcance é PRT e equivale ao MOV. (1PM) Ganha +2 em FD por esquiva (não cumulativo com Aceleração) e ignora Dano de Impacto.
2PT Pode carregar consigo o que puder segurar em peso. O alcance aumenta para LNG. 
ATV: (2PM) Pode teleportar para locais não visíveis testando H: se falhar, nada acontece (sem gasto de PM); se for bem-sucedido, teleporta-se normalmente. (2PM) Pode levar um personagem JNT por ponto de R. Personagens resistem com RxH; aqueles com R igual ou superior não podem ser teleportados contra a vontade. (2PM) Ao usar a Manobra Especial: Finta, ignora a disputa de H.
3PT Pode teleportar para qualquer local já visitado, sem limite de distância. Personagens com R superior podem ser teleportados contra a vontade (testam RxH para resistir). (10PM) Pode transportar todos os aliados para um local seguro próximo (definido pelo mestre), sem perda de Pontos de Experiência. (+3PM) Ao usar teleporte como esquiva, recebe 1d bônus na FD.
TOQUE DE ENERGIA
Canaliza pela própria pele uma poderosa carga de energia.
O Tipo de Dano deve ser escolhido na aquisição da vantagem e não pode ser alterado (mas é possível gastar 1 Ponto de Experiência para obter um novo Tipo de Dano). Danos físicos (Contusão e Lacerante) ficam a critério do mestre.
1PT Pode realizar um ataque com energia corporal em um turno, com FA = H + A + 1d6 (dobrando A em crítico), contra um alvo JNT. Se o alvo absorver toda a FA, não sofre Dano de Impacto. (1PM) Realiza um ataque contra todos os personagens JNT, mesmo sem contato direto, com FA baseada em A (ex.: A3 = 1d+3).
2PT Ao realizar um ataque de area, afeta todos os personagens PRT, incluindo aliados. (2PM) Pode aplicar efeito atordoante: todos que perderem ao menos 1PV devem testar AxA ou ficam atordoados. (2PM) Pode envolver o corpo em energia (não consome ACT, dura até o fim da CNA): causa 1PV de dano a qualquer um que o toque ou o ataque fisicamente. Personagens com Armadura Extra ao Tipo de Dano são imunes; se tiverem Vulnerável, sofrem 2PV. Seus ataques físicos (F) passam a causar dano do Tipo de Dano, sem redutores.
3PT Pode sacrificar PV para aumentar a FA, na proporção de +1 FA por 1PV. Se gastar mais da metade dos PV e cair a 0 PV sem ter feito um descanso total no último PRD, morre automaticamente. (3PM) Se estiver ferido, pode converter a quantidade de PV perdidos em bônus de FA (limite: seu valor de A). (3PM) Pode adicionar o valor de R como bônus na FA.
TOQUE ESPECIAL
Você interage com objetos e personagens à distância, seja por membros elásticos, telecinese ou habilidade com chicote.
1PT Usa F PRT, calcula a distância em tabuleiros como se tivesse PDF equivalente a metade da F (arredondada para cima). Testa H apenas para alvos não visíveis, mas conhecidos. (1PM) Estende os sentidos (visão, audição), em certos casos até voz, até o alcance, usando MOV.
2PT Alcance em tabuleiros passa a ser calculado com PDF igual a F. (2PM) Pode interagir com qualquer coisa além de PRT, contanto que saiba de alguma forma sua localização. Cada aumento de distância conta como uma utilização (2PM para alcançar algo LNG; +2PM no próximo turno para alcançar FDA).
TRANSFORMAÇÃO
Você pode alterar sua aparência.
1PT (1PM) Pode modificar aspectos sutis, como cor do cabelo, olhos e pele, alterar feições faciais e ajustar ligeiramente altura ou peso. A mudança ocorre em um turno e dura indefinidamente, exceto se ficar inconsciente ou morrer. Concede +1 em Ladinagem e Persuasão, dependendo da forma ou contexto. Não pode copiar a aparência de outro personagem nem alterar sua aparência a ponto de parecer ter outra ancestralidade.
2PT Escolha uma ancestralidade cuja aparência possa copiar, sem obter benefícios. (2PM) Pode modificar sua forma de maneira mais drástica, adquirindo Vantagens de 1PT em um turno com autorização do mestre. (+1PM) O bônus em PRC aumenta em +1, até o limite de +2.
3PT Escolha uma ancestralidade cuja aparência possa copiar. O bônus máximo em PRC aumenta para +3. (+3PM) Ao adquirir Vantagem por mudança de forma, esta terá 2PT. (3PM) Pode alterar sua forma em detalhes mínimos, redistribuindo até 3PT entre seus Atributos, por 1 CNA. (1PM/Turno) Pode copiar a forma de outro ser dentro de seu campo de visão em um turno.
VOO
Você pode voar.
Seu voo equivale a metade de sua velocidade máxima, quase como uma levitação. (1PM) Pode utilizar sua velocidade máxima em voo no seu turno.
XAMÃ
Você tem uma forte ligação com o mundo dos espíritos.
1PT Pode ver, interagir e atacar criaturas incorpóreas (e ser atacado por elas). Pode utilizar o Sentido Especial: Ver o Invisível, mas apenas com seres com a maldição Incorpóreo. Percebe em um turno se um personagem está Assombrado, sob Possessão ou Telepatia. (1PM) Pode se comunicar com espíritos de outros planos via rituais. O mestre decide se haverá resposta. 
2PT Ao ter um objeto de um morto, pode consultá-lo com concentração e nunca será possuído por ele. Pode eliminar uma possessão, controle mental ou similar, com uma disputa de RxR. (2PM) Pode ver auras: cada uso revela uma Vantagem ou Desvantagem (não nomeadas), conforme cor, frequência e intensidade. Também mede a força espiritual de personagens.
3PT Imune a Possessão, exceto durante o sono. (3PM) Pode incorporar espíritos (1 CNA), ganhando +1PT em Atributos ou Vantagens por espírito, até o máximo de +3PT por 9PM. (3PM) Ao matar um ser, absorve sua alma e adquire temporariamente uma de suas Vantagens (1 CNA). Só se pode absorver uma alma e um poder por vez. (3PM) Pode realizar uma Viagem Astral, separando mente e corpo. A consciência torna-se invisível, intangível e só pode ser percebida com Ver o Invisível. É afetado apenas por poderes mentais ou místicos. Pode tornar-se parcialmente visível caso queira. Pode usar poderes mentais, invadir sonhos e espionar livremente. A viagem dura indefinidamente e possui alcance ilimitado. O corpo físico permanece em coma, vulnerável, mas o Xamã sente se ele for ferido. Retorno ao corpo é instantâneo.
`;

const part1 = fs.readFileSync('rules_raw.txt', 'utf8');

// Truncate part1 at MANOBRAS ESPECIAIS
const splitMarker = "MANOBRAS ESPECIAIS";
const splitIndex = part1.indexOf(splitMarker);

let fullText;
if (splitIndex !== -1) {
    fullText = part1.substring(0, splitIndex) + part2;
} else {
    // If not found, maybe append? But it should be found based on file reading.
    // Actually, rules_raw.txt has MANOBRAS ESPECIAIS near the end.
    // Check if it's found.
    fullText = part1 + "\n" + part2; 
    console.log("Warning: Split marker not found in part 1, appending.");
}

// Replacements
const replacements = {
    "\\bJNT\\b": "Junto",
    "\\bPRT\\b": "Perto",
    "\\bLNG\\b": "Longe",
    "\\bFDA\\b": "Fora do Alcance",
    "\\bACT\\b": "Ação",
    "\\bMOV\\b": "Movimento",
    "\\bINI\\b": "Iniciativa",
    "\\bCNA\\b": "Cena",
    "\\bSTN\\b": "Status Negativo",
    "\\bEQP\\b": "Equipamento",
    "\\bPDR\\b": "Poder",
    "\\bPRC\\b": "Perícia",
    "\\bATV\\b": "Poder Ativo",
    "\\bPSV\\b": "Poder Passivo",
    "\\bTEC\\b": "Técnica"
};

let processedText = fullText;

for (const [abbr, fullWord] of Object.entries(replacements)) {
    const regex = new RegExp(abbr, 'g');
    processedText = processedText.replace(regex, fullWord);
}

// Parsing
const lines = processedText.split('\n');
let kotlinCode = "    private fun getAdvantages(): MutableList<ItemDefinition> {\n";
kotlinCode += "        val list = mutableListOf<ItemDefinition>()\n\n";

let currentName = "";
let currentBody = "";

for (let line of lines) {
    line = line.trim();
    if (!line) continue;

    const isHeader = (line.toUpperCase() === line && !line.match(/^\d/) && line.length > 2);
    
    // Additional check: some lines in body might be uppercase, e.g. "ATV:", "TEC".
    // Headers are mostly single words or few words.
    // "MANOBRAS ESPECIAIS" is header.
    // "ATV:" is NOT a header for a new item.
    // "TEC" is NOT a header (it's inside proper text usually).
    // The list of headers is distinct.
    
    if (isHeader && !line.includes(":") && line !== "ATV" && line !== "PSV") {
         if (currentName) {
             addEntry(currentName, currentBody);
         }
         currentName = line;
         currentBody = "";
    } else {
        currentBody += line + "\n";
    }
}

if (currentName) {
    addEntry(currentName, currentBody);
}

kotlinCode += "\n        return list\n    }\n";

function addEntry(name, body) {
    let cost = "1-3";
    if (body.includes("1PT")) {
        if (body.includes("5PT") || body.includes("Custo básico 1pt")) cost = "1-5";
        else if (body.includes("3PT")) cost = "1-3";
        else if (body.includes("2PT")) cost = "1-2";
        else cost = "1";
        
        if (body.includes("Varia") || body.includes("custo em PT")) cost = "Varia";
    }
    
    if (name === "EQUIPAMENTO ESPECIAL") cost = "1";
    if (name === "MANOBRAS ESPECIAIS") cost = "1";
    if (name === "QUALIDADES ESPECIAIS") cost = "1";
    if (name === "SENTIDOS ESPECIAIS") cost = "1";
    if (name === "RESTAURAÇÃO") cost = "1";
    if (name === "VOO") cost = "1";

    const bodyEscaped = body.replace(/"/g, '\\"').replace(/\n/g, '\\n');
    // Also escape $ because Kotlin strings interpolate it
    const bodyFinal = bodyEscaped.replace(/\$/g, '\\$');
    
    kotlinCode += `        list.add(ItemDefinition(name = "${name}", cost = "${cost}", description = "${bodyFinal}"))\n`;
}

fs.writeFileSync('GaidenData_generated.kt', kotlinCode);
console.log("Kotlin code generated successfully.");
