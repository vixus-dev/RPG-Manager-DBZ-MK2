# RPG Manager Dragon Ball Z [MK2]

Segunda versão do RPG Manager, uma ferramenta desktop desenvolvida em **Java Swing** para auxiliar mestres de RPG de mesa a gerenciar personagens, combates e recursos em tempo real — com temática **Dragon Ball Z**.
Esta versão é a evolução direta do [MK1](https://github.com/vixus-dev/RPG-Manager-DBZ-MK1), com arquitetura completamente refatorada e diversas novas mecânicas de combate.
```
# ⚔️ O que mudou do MK1 para o MK2?
O MK1 foi uma prova de conceito onde toda a lógica estava concentrada em uma única classe. O objetivo era validar as ideias centrais rapidamente, sem preocupação com organização do código.
O MK2 representa a maturação do projeto:

|                                            | MK1|                               | MK2 |
| Arquitetura                   | Monolítica (1 classe principal)       | Modular (9 classes de interface + 12 classes de modelo) |
| Classes Java                  | 3                                     | 21 |
| Total de linhas               | ~2.200                                | ~3.400 |
| Formato de dados              | `.txt`                                | `.json` (via Gson) |
| Modelos de dados              | Inner classes dentro do RPGManager    | Pacote dedicado `com.Vixus.Library` |
| Log de combate                | Sem log                               | Janela de log separada e persistente |
| Editor de inimigos            | Sem editor dedicado                   | `EditorDeInimigos.java` separado |
| Sistema de Clash              | Não tem                               | ✅ Sistema completo de Clash por turnos |
| Efeitos de Status             | Não tem                               | ✅ `StatusEffect` com duração e efeitos por turno |
| Sincronia entre personagens   | Não tem                               | ✅ `SyncGroup` para ações sincronizadas |
| Fusões                        | Não tem                               | ✅ Fusões ativas entre jogadores |
| Ataques em Área (AoE)         | Não tem                               | ✅ Resolução de esquiva individual por alvo |
| Sub-formas de transformação   | Não tem                               | ✅ `SubForma` dentro das transformações |
| Graus de ampliação            | Não tem                               | ✅ `GrauAmpliacao` com múltiplos níveis |
| Cores por transformação       | Não tem                               | ✅ Configurável via `cores.json` |
| Bestiário                     | Não tem                               | ✅ `Bestiario.json` com inimigos pré-definidos |
```
---
```
## 🗂️ Estrutura do Projeto
src/
├── com/Vixus/inc/          # Interface Gráfica (Swing)
│   ├── RPGManager.java              # Janela principal
│   ├── EditorDePersonagens.java     # Editor de fichas dos jogadores
│   ├── EditorDeInimigos.java        # Editor de fichas dos inimigos
│   ├── JanelaDeLog.java             # Log de combate em tempo real
│   ├── AoEDodgeResolverDialog.java  # Resolução de esquiva em ataques de área
│   ├── StatusCellRenderer.java      # Renderizador visual da tabela de status
│   ├── JTableComTooltips.java       # Tabela com tooltips
│   ├── CheckboxListCellRenderer.java
│   └── TransparentLineBorder.java
│
└── com/Vixus/Library/      # Modelos de Dados
    ├── Personagem.java      # Classe base para Jogador e Inimigo
    ├── Jogador.java
    ├── Inimigo.java
    ├── Transformacao.java / SubForma.java
    ├── Ampliacao.java / GrauAmpliacao.java
    ├── Tecnica.java
    ├── Item.java
    ├── StatusEffect.java    # Efeitos com duração (ex: Regeneração)
    ├── SyncGroup.java       # Grupos de sincronia entre personagens
    └── PersonagensData.java

Resources/
    ├── personagens.json     # Fichas dos jogadores e inimigos ativos
    ├── Bestiario.json       # Catálogo de inimigos pré-definidos
    ├── transformacoes.json
    ├── ampliacoes.json
    ├── itens.json
    └── cores.json           # Cores personalizadas por transformação/ampliação
```
```

---

## ✨ Funcionalidades

- Tabela de atributos em tempo real para jogadores e inimigos
- Sistema de combate por turnos com barra de ordem de turno
- Sistema de Clash entre dois personagens com duração configurável
- Ataques em Área (AoE) com resolução de esquiva individual por alvo
- Transformações com sub-formas e impacto visual por cor customizável
- Ampliações com múltiplos graus
- Efeitos de Status com duração em turnos (ex: Regeneração de HP)
- Sincronia de personagens (`SyncGroup`) para ações coordenadas
- Fusões entre jogadores
- Bestiário de inimigos pré-configurados
- Janela de Log dedicada com histórico de todas as ações de combate
- Editor de personagens e editor de inimigos separados
- Persistência de dados em JSON via biblioteca Gson
```

## 🛠️ Tecnologias

- Java
- Swing (Interface Gráfica)
- Gson (Serialização/Desserialização de JSON)
