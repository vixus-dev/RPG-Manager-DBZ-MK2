package com.Vixus.inc;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.Vixus.Library.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.math.BigDecimal;
import java.math.BigInteger;

public class RPGManager extends JFrame {

	private JanelaDeLog janelaDeLog;
    private JTable tabelaAtributos;
    private DefaultTableModel modeloTabela;
    private JLabel turnOrderBar;
    private int indiceTurnoAtual = 0;
    private java.util.List<com.Vixus.Library.Personagem> listaDeTurnos = new java.util.ArrayList<>();
    
    private boolean emClash = false;
    private com.Vixus.Library.Personagem[] participantesClash = new com.Vixus.Library.Personagem[2];
    private int turnosRestantesClash = 0;
    private int indiceClashAtual = 0; // 0 para o primeiro participante, 1 para o segundo

    // Listas de dados principais
    private List<Jogador> listaJogadores = new ArrayList<>();
    private List<Inimigo> listaInimigos = new ArrayList<>();
    private List<Inimigo> listaInimigosMestre = new ArrayList<>();
    private List<Transformacao> listaTransformacoes = new ArrayList<>();
    private List<Ampliacao> listaAmpliacoes = new ArrayList<>();
    private List<Tecnica> listaTecnicas = new ArrayList<>();
    private final Map<String, Jogador[]> fusoesAtivas = new HashMap<>();;
    private List<Item> listaItensMestre = new ArrayList<>();
    private Map<String, Color> coresTransformacoes = new HashMap<>();
    private Map<String, Color> coresAmpliacoes = new HashMap<>();
    
    private List<SyncGroup> syncGroupsAtivos = new ArrayList<>();
    private List<Color> coresDisponiveisParaSincronia = new ArrayList<>(List.of(
        Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.PINK, Color.ORANGE
    ));
    private int proximaCorIndex = 0;
    
    private boolean mostrarApenasAtivos = false;

    public static class PersonagensData {
        List<Jogador> jogadores;
        List<Inimigo> inimigos;
    }

    public RPGManager() {
        setTitle("RPG Manager");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600); 
        setLocationRelativeTo(null);

        JPanel painel = criarPainelUnificado();
        add(painel);
        
        janelaDeLog = new JanelaDeLog(this);

	    importarDados();
        atualizarTabela();
        
        StatusCellRenderer renderer = new StatusCellRenderer(this, coresTransformacoes, coresAmpliacoes);
        for (int i = 0; i < tabelaAtributos.getColumnCount(); i++) {
            tabelaAtributos.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        
        tabelaAtributos.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) { // Verifica se é um clique-direito
                    int linha = tabelaAtributos.rowAtPoint(e.getPoint());
                    if (linha >= 0 && linha < tabelaAtributos.getRowCount()) {
                        
                        String tipo = (String) tabelaAtributos.getValueAt(linha, 0);
                        // O menu só aparecerá para Jogadores
                        if ("Jogador".equals(tipo)) {
                            String nome = (String) tabelaAtributos.getValueAt(linha, 1);
                            Jogador jogadorSelecionado = buscarJogadorPorNome(nome);

                            if (jogadorSelecionado != null) {
                                JPopupMenu menu = new JPopupMenu();
                                JMenuItem itemAtivoInativo = new JMenuItem(
                                    jogadorSelecionado.isAtivoNaSessao() ? "Marcar como Inativo" : "Marcar como Ativo"
                                );
                                
                                itemAtivoInativo.addActionListener(evt -> {
                                    // Inverte o estado atual
                                    jogadorSelecionado.setAtivoNaSessao(!jogadorSelecionado.isAtivoNaSessao());
                                    atualizarTabela(); // Redesenha a tabela para refletir a mudança
                                });
                                
                                menu.add(itemAtivoInativo);
                                menu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    }
                }
            }
        });
    }
    
    private void log(String mensagem) {
        // Envia a mensagem para a nossa nova janela de log
        if (janelaDeLog != null) {
            janelaDeLog.adicionarMensagem(mensagem);
        }
        
        JOptionPane.showMessageDialog(this, mensagem);
    }
    
    public static String formatarNumero(BigInteger numero) {
        if (numero == null) return "0";
        if (numero.compareTo(new BigInteger("1000")) < 0) {
            return numero.toString();
        }

        TreeMap<BigInteger, String> map = new TreeMap<>();
        map.put(new BigInteger("1000"), "K");
        map.put(new BigInteger("1000000"), "M");
        map.put(new BigInteger("1000000000"), "B");
        map.put(new BigInteger("1000000000000"), "T");
        map.put(new BigInteger("1000000000000000"), "Qa");
        map.put(new BigInteger("1000000000000000000"), "Qt");
        map.put(new BigInteger("1000000000000000000000"), "Sx");
        map.put(new BigInteger("1000000000000000000000000"), "Sp");
        map.put(new BigInteger("1000000000000000000000000000"), "Oc");
        map.put(new BigInteger("1000000000000000000000000000000"), "No");
        map.put(new BigInteger("1000000000000000000000000000000000"), "Dc");
  

        Map.Entry<BigInteger, String> entry = map.floorEntry(numero);
        if (entry == null) return numero.toString();

        BigInteger divisor = entry.getKey();
        String sufixo = entry.getValue();

        BigInteger parteInteira = numero.divide(divisor);
        BigInteger resto = numero.remainder(divisor);
        BigInteger parteDecimal = resto.divide(divisor.divide(BigInteger.TEN));

        if (parteDecimal.equals(BigInteger.ZERO)) {
            return parteInteira + sufixo;
        } else {
            return parteInteira + "." + parteDecimal + sufixo;
        }
    }

    private JPanel criarPainelUnificado() {
        JPanel painel = new JPanel(new BorderLayout(0,10));
        
        turnOrderBar = new JLabel("Calculando ordem de turno...", JLabel.CENTER); // Agora é um JLabel
        turnOrderBar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        painel.add(turnOrderBar, BorderLayout.NORTH);

        modeloTabela = new DefaultTableModel(new String[]{"Tipo", "Nome", "HP", "ATK", "Energia/Bateria", "Velocidade", "Transformação", "Ampliação", "Custo p/ Turno" ,"Pontos Forma"}, 0);
        tabelaAtributos = new JTableComTooltips(modeloTabela, this);
        
        // --- AUMENTAR O TAMANHO DA FONTE E ALTURA DA LINHA DA TABELA ---
        // Tamanho com 2 telas -> 30
        // Tamanho com 1 tela -> 23
        tabelaAtributos.setFont(new Font("Comic Sans MS", Font.PLAIN, 23));

        // Tamanho com 2 telas -> 40
        // Tamanho com 1 tela -> 36
        tabelaAtributos.setRowHeight(36);

        // Fonte do cabeçalho com 2 telas -> 25
        // Fonte do cabeçalho com 1 tela -> 16
        tabelaAtributos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 15));
        painel.add(new JScrollPane(tabelaAtributos), BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel();
        JButton btnIniciarTurno = new JButton("Iniciar Turno");
        JButton btnTransformar = new JButton("Transformar/Ampliar");
        JButton btnGerenciarInimigos = new JButton("Gerenciar Inimigos");
        JButton btnInventario = new JButton("Inventário");
        JButton btnFusões = new JButton("Fusões");
        JButton btnEditor = new JButton("Editor de Personagens");
        
        JToggleButton btnOcultarInativos = new JToggleButton("Mostrar Apenas Ativos");
        btnOcultarInativos.addActionListener(e -> {
            // Inverte o estado do filtro e atualiza a tela
            mostrarApenasAtivos = btnOcultarInativos.isSelected();
            atualizarTabela();
        });

        // Configuração dos Listeners
        btnIniciarTurno.addActionListener(e -> abrirPopupTurno());
        btnTransformar.addActionListener(e -> abrirPopupFormas());
        btnGerenciarInimigos.addActionListener(e -> abrirPopupGerenciarInimigos());
        btnInventario.addActionListener(e -> abrirPopupInventario());
        btnFusões.addActionListener(e -> abrirPopupFusoes());
        btnEditor.addActionListener(e -> new EditorDePersonagens(this, listaJogadores).setVisible(true));

        // Adicionar botões ao painel
        painelBotoes.add(btnIniciarTurno);
        painelBotoes.add(btnTransformar);
        painelBotoes.add(btnGerenciarInimigos);
        painelBotoes.add(btnInventario);
        painelBotoes.add(btnFusões);
        painelBotoes.add(btnEditor);
        painelBotoes.add(btnOcultarInativos);

        painel.add(painelBotoes, BorderLayout.SOUTH);
        return painel;
    }
    
    public void proximoTurno() {
    	
    	if (emClash) {
            turnosRestantesClash--;
            if (turnosRestantesClash <= 0) {
                finalizarClash();
            } else {
                // Alterna o índice entre 0 e 1 para trocar o turno entre os participantes
                indiceClashAtual = 1 - indiceClashAtual; 
                atualizarTabela();
            }
            return; // Pula a lógica de turno normal
        }
    	
        indiceTurnoAtual++;
        if (indiceTurnoAtual >= listaDeTurnos.size()) {
            indiceTurnoAtual = 0;
        }
        atualizarTabela(); 
    }
    
    private void atualizarOrdemDeTurno() {
        listaDeTurnos.clear();
        
        if (mostrarApenasAtivos) {
            listaJogadores.stream()
                .filter(Jogador::isAtivoNaSessao)
                .forEach(listaDeTurnos::add);
        } else {
            listaDeTurnos.addAll(listaJogadores);
        }

        listaDeTurnos.addAll(listaInimigos);

        if (emClash) {
            StringBuilder sb = new StringBuilder("<html><div style='text-align: center; color:purple;'>CLASH: ");
            Personagem ativo = getPersonagemAtivo();
            
            sb.append("<b style='color:red;'><u>&nbsp;Turno de ").append(ativo.getNome()).append("&nbsp;</u></b>");
            sb.append(" (Restam ").append(turnosRestantesClash).append(" turnos)");
            sb.append("</div></html>");
            turnOrderBar.setText(sb.toString());
            return; // Pula a lógica de ordenação normal
        }
        
        listaDeTurnos.sort((p1, p2) -> {
            int comparacaoVelocidade = p2.getVelocidade().compareTo(p1.getVelocidade());
            if (comparacaoVelocidade == 0) {
                return p1.getNome().compareToIgnoreCase(p2.getNome());
            }
            return comparacaoVelocidade;
        });

        StringBuilder sb = new StringBuilder("<html><div style='text-align: center;'>");
        for (int i = 0; i < listaDeTurnos.size(); i++) {
            com.Vixus.Library.Personagem p = listaDeTurnos.get(i);

            // Se o personagem é o do turno atual, aplica a formatação especial
            if (i == indiceTurnoAtual) {
                sb.append("<font color='blue'><b>【").append(p.getNome()).append("】</b></font>");
            } else {
                sb.append(p.getNome());
            }

            if (i < listaDeTurnos.size() - 1) {
                sb.append("  ->  ");
            }
        }
        sb.append("</div></html>");

        // Atualiza o texto do JLabel
        turnOrderBar.setText(sb.toString());
    }
    
    private void importarDados() {
        Gson gson = new Gson();
        try {
            // Define o caminho para a pasta de recursos
            String resourcePath = "src/com/Vixus/Resources/";

         // Carrega a nova lista de itens mestre do JSON
            Reader readerItens = Files.newBufferedReader(Paths.get(resourcePath + "itens.json"));
            java.lang.reflect.Type tipoListaItem = new TypeToken<ArrayList<Item>>() {}.getType();
            this.listaItensMestre = gson.fromJson(readerItens, tipoListaItem);
            
            // Carrega as Transformações do JSON
            Reader readerTransformacoes = Files.newBufferedReader(Paths.get(resourcePath + "transformacoes.json"));
            java.lang.reflect.Type tipoListaTransformacao = new TypeToken<ArrayList<Transformacao>>() {}.getType();
            this.listaTransformacoes = gson.fromJson(readerTransformacoes, tipoListaTransformacao);

            // Carrega as Ampliações do JSON
            Reader readerAmpliacoes = Files.newBufferedReader(Paths.get(resourcePath + "ampliacoes.json"));
            java.lang.reflect.Type tipoListaAmpliacao = new TypeToken<ArrayList<Ampliacao>>() {}.getType();
            this.listaAmpliacoes = gson.fromJson(readerAmpliacoes, tipoListaAmpliacao);

            Reader readerBestiario = Files.newBufferedReader(Paths.get(resourcePath + "bestiario.json"));
            java.lang.reflect.Type tipoListaInimigo = new TypeToken<ArrayList<Inimigo>>() {}.getType();
            this.listaInimigosMestre = gson.fromJson(readerBestiario, tipoListaInimigo);

            // Carrega os Personagens (Jogadores e Inimigos) do JSON
            Reader readerPersonagens = Files.newBufferedReader(Paths.get(resourcePath + "personagens.json"));
            PersonagensData data = gson.fromJson(readerPersonagens, PersonagensData.class);
            this.listaJogadores = Optional.ofNullable(data.jogadores).orElse(new ArrayList<>());
            this.listaInimigos = Optional.ofNullable(data.inimigos).orElse(new ArrayList<>());

            // Inicializa os status "atuais" dos personagens após o carregamento
            this.listaJogadores.forEach(Jogador::inicializarAposLoad);
            this.listaInimigos.forEach(Inimigo::inicializarAposLoad);
            
            //importa e veincula as Habilidades/Tecnicas de cada Jogador
            importarTecnicas(resourcePath + "tecnicas.txt");
            vincularHabilidades();
            
            Reader readerCores = Files.newBufferedReader(Paths.get(resourcePath + "cores.json"));
            // Usamos um tipo genérico para que o Gson entenda a estrutura do JSON
            java.lang.reflect.Type tipoMapaCores = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
            Map<String, Map<String, String>> dadosCores = gson.fromJson(readerCores, tipoMapaCores);
            
            // Converte as strings "R,G,B" em objetos Color
            dadosCores.get("transformacoes").forEach((nome, rgb) -> {
                String[] c = rgb.split(",");
                coresTransformacoes.put(nome, new Color(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])));
            });
            dadosCores.get("ampliacoes").forEach((nome, rgb) -> {
                String[] c = rgb.split(",");
                coresAmpliacoes.put(nome, new Color(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])));
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro Crítico ao carregar arquivos de dados: " + e.getMessage(), "Erro de Carga", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1); // Fecha o programa se os dados essenciais não puderem ser carregados
        }
    }
    
    public Transformacao buscarTransformacaoPai(SubForma subForma) {
        return listaTransformacoes.stream()
                .filter(t -> t.getSubFormas().contains(subForma))
                .findFirst().orElse(null);
    }

    public Ampliacao buscarAmpliacaoPai(GrauAmpliacao grau) {
        return listaAmpliacoes.stream()
                .filter(a -> a.getGraus().contains(grau))
                .findFirst().orElse(null);
    }

    private void vincularHabilidades() {
    	    for (Jogador j : listaJogadores) {
    	            // Vincula Transformações
    	            if (j.getNomesTransformacoes() != null) {
    	                List<Transformacao> aprendidas = new ArrayList<>();
    	                for (String nomeForma : j.getNomesTransformacoes()) {
    	                    listaTransformacoes.stream()
    	                        .filter(t -> t.getNome().equals(nomeForma))
    	                        .findFirst()
    	                        .ifPresent(aprendidas::add);
    	                }
    	                j.setTransformacoesDisponiveis(aprendidas);
    	            }
    	            // Vincula Ampliações
    	            if (j.getNomesAmpliacoes() != null) {
    	                List<Ampliacao> aprendidas = new ArrayList<>();
    	                for (String nomeForma : j.getNomesAmpliacoes()) {
    	                    listaAmpliacoes.stream()
    	                        .filter(a -> a.getNome().equals(nomeForma))
    	                        .findFirst()
    	                        .ifPresent(aprendidas::add);
    	                }
    	                j.setAmpliacoesDisponiveis(aprendidas);
    	            }
    	            if (j.getNomesTecnicas() != null) {
    	                List<Tecnica> aprendidas = new ArrayList<>();
    	                for (String nomeTecnica : j.getNomesTecnicas()) {
    	                    listaTecnicas.stream()
    	                        .filter(t -> t.getNome().equals(nomeTecnica))
    	                        .findFirst()
    	                        .ifPresent(aprendidas::add);
    	                }
    	                j.setTecnicasDisponiveis(aprendidas);
    	            }
    	            if (j.getRegeneracaoPassiva() != null && j.getRegeneracaoPassiva().compareTo(BigDecimal.ZERO) > 0) {
    	                BigDecimal hpMax = new BigDecimal(j.getHpMaximoAtual());
    	                BigDecimal percentualCura = j.getRegeneracaoPassiva();
    	                BigInteger curaPorTurno = hpMax.multiply(percentualCura).toBigInteger();
    	                
    	                StatusEffect efeitoRegen = new StatusEffect("Regeneração de HP", Integer.MAX_VALUE, curaPorTurno);
    	                log(j.getNome() + " curou " + curaPorTurno + " neste turno.");
    	                j.getEfeitosAtivos().add(efeitoRegen);
    	            }
    	    }

    	    for (Inimigo i : listaInimigos) {
    	            // Vincula Transformações
    	            if (i.getNomesTransformacoes() != null) {
    	                List<Transformacao> aprendidas = new ArrayList<>();
    	                for (String nomeForma : i.getNomesTransformacoes()) {
    	                     listaTransformacoes.stream().filter(t -> t.getNome().equals(nomeForma)).findFirst().ifPresent(aprendidas::add);
    	                }
    	                i.setTransformacoesDisponiveis(aprendidas);
    	            }
    	            // Vincula Ampliações
    	            if (i.getNomesAmpliacoes() != null) {
    	                List<Ampliacao> aprendidas = new ArrayList<>();
    	                for (String nomeForma : i.getNomesAmpliacoes()) {
    	                    listaAmpliacoes.stream().filter(a -> a.getNome().equals(nomeForma)).findFirst().ifPresent(aprendidas::add);
    	                }
    	                i.setAmpliacoesDisponiveis(aprendidas);
    	            }
    	            if (i.getNomesTecnicas() != null) {
    	                List<Tecnica> aprendidas = new ArrayList<>();
    	                for (String nomeTecnica : i.getNomesTecnicas()) {
    	                    listaTecnicas.stream()
    	                        .filter(t -> t.getNome().equals(nomeTecnica))
    	                        .findFirst()
    	                        .ifPresent(aprendidas::add);
    	                }
    	                i.setTecnicasDisponiveis(aprendidas);
    	            }
    	            if (i.getRegeneracaoPassiva() != null && i.getRegeneracaoPassiva().compareTo(BigDecimal.ZERO) > 0) {
    	                BigDecimal hpMax = new BigDecimal(i.getHpMaximoAtual());
    	                BigDecimal percentualCura = i.getRegeneracaoPassiva();
    	                BigInteger curaPorTurno = hpMax.multiply(percentualCura).toBigInteger();
    	                
    	                // Cria um efeito "permanente" de regeneração
    	                StatusEffect efeitoRegen = new StatusEffect("Regeneração de HP", Integer.MAX_VALUE, curaPorTurno);
    	                log(i.getNome() + " curou " + curaPorTurno + " neste turno.");
    	                i.getEfeitosAtivos().add(efeitoRegen);
    	            }
    	        }
    	    }
    
    private void atualizarTabela() {
        modeloTabela.setRowCount(0);
        
        List<Jogador> jogadoresParaMostrar = listaJogadores;
        if (mostrarApenasAtivos) {
            jogadoresParaMostrar = listaJogadores.stream()
                    .filter(Jogador::isAtivoNaSessao)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        for (Jogador j : jogadoresParaMostrar) {
            String transformacaoInfo = j.getSubFormaAtual() != null ? j.getSubFormaAtual().getNome() : "-";
            String ampliacaoInfo = j.getGrauAmpliacaoAtual() != null ? j.getGrauAmpliacaoAtual().getNome() : "-";
            String displayEnergiaBateria = j.isAndroide() ? String.format("%.1f%%", j.getBateria()) : formatarNumero(j.getEnergia());
            String displayForma = j.isFormaIlimitada() ? "∞" : formatarNumero(j.getFormaPontos());

            modeloTabela.addRow(new Object[]{
                "Jogador", j.getNome(), formatarNumero(j.getHP()), formatarNumero(j.getAtk()),
                displayEnergiaBateria, formatarNumero(j.getVelocidade()),
                transformacaoInfo, ampliacaoInfo, j.getCustoAtualPorTurno() ,displayForma
            });
        }
        for (Inimigo i : listaInimigos) {
            String transformacaoInfo = "-";
            if (i.getSubFormaAtual() != null) {
                // CORREÇÃO: Usa o novo método para pegar o nome da subforma
                transformacaoInfo = i.getSubFormaAtual().getNome();
            }

            String ampliacaoInfo = "-";
            if (i.getGrauAmpliacaoAtual() != null) {
                ampliacaoInfo = i.getGrauAmpliacaoAtual().getNome();
            }

            modeloTabela.addRow(new Object[]{
                "Inimigo",
                i.getNome(),
                formatarNumero(i.getHp()),
                formatarNumero(i.getAtk()),
                formatarNumero(i.getEnergia()),
                formatarNumero(i.getVelocidade()),
                transformacaoInfo,      // Exibe a informação correta
                ampliacaoInfo,          // Exibe a informação correta
                i.getCustoAtualPorTurno(),
                formatarNumero(i.getFormaPontos())
            });
        }
        atualizarOrdemDeTurno();
    }
    
    private void iniciarClash(com.Vixus.Library.Personagem atacante, com.Vixus.Library.Personagem alvo) {
        this.emClash = true;
        this.turnosRestantesClash = 6;
        // O atacante original sempre começa o Clash
        this.participantesClash[0] = atacante;
        this.participantesClash[1] = alvo;
        this.indiceClashAtual = 0; 

        /*JOptionPane.showMessageDialog(this, "Dois guerreiros formidaveis entraram em um CLASH!\n" +
                atacante.getNome() + " e " + alvo.getNome() + " estão travados em combate por 6 turnos!",
                "CLASH INICIADO", JOptionPane.WARNING_MESSAGE);*/
        log("inicio do Clash entre: " + atacante.getNome() + " e " + alvo.getNome());
        
        atualizarTabela(); // Atualiza a UI para refletir o novo estado de Clash
    }
    
    private void finalizarClash() {
        //JOptionPane.showMessageDialog(this, "O Clash terminou!", "Fim do Clash", JOptionPane.INFORMATION_MESSAGE);
        log("Fim do Clash");
        
        this.emClash = false;
        this.turnosRestantesClash = 0;
        
        // Encontra o índice do último personagem ativo do clash na lista de turnos normal
        // para que o jogo continue a partir daquele ponto.
        com.Vixus.Library.Personagem ultimoAtivo = participantesClash[indiceClashAtual];
        this.indiceTurnoAtual = listaDeTurnos.indexOf(ultimoAtivo);

        // Limpa os dados do clash
        this.participantesClash[0] = null;
        this.participantesClash[1] = null;
        
        atualizarTabela();
    }
    
    private com.Vixus.Library.Personagem getPersonagemAtivo() {
        if (emClash) {
            return participantesClash[indiceClashAtual];
        } else if (!listaDeTurnos.isEmpty()) {
            return listaDeTurnos.get(indiceTurnoAtual);
        }
        return null;
    }
    
    private void abrirPopupGerenciarInimigos() {
        JDialog popup = new JDialog(this, "Gerenciar Inimigos", true);
        popup.setSize(500, 450);
        popup.setLocationRelativeTo(this);
        popup.setLayout(new BorderLayout(10, 10));

        // --- PAINEL CENTRAL: LISTA DE INIMIGOS EM COMBATE ---
        popup.add(new JLabel("Inimigos em Combate:"), BorderLayout.NORTH);
        DefaultListModel<String> modeloListaEmCombate = new DefaultListModel<>();
        JList<String> listaEmCombate = new JList<>(modeloListaEmCombate);
        listaInimigos.forEach(inimigo -> modeloListaEmCombate.addElement(inimigo.getNome()));
        popup.add(new JScrollPane(listaEmCombate), BorderLayout.CENTER);

        // --- PAINEL SUL: AÇÕES ---
        JPanel painelAcoes = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Seção: Adicionar a partir de Modelo ---
        gbc.gridy = 0;
        gbc.gridx = 0; painelAcoes.add(new JLabel("Modelo:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2; // Ocupa 2 colunas
        JComboBox<Inimigo> comboModelos = new JComboBox<>(listaInimigosMestre.toArray(new Inimigo[0]));
        painelAcoes.add(comboModelos, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1; // Volta a ocupar 1 coluna
        painelAcoes.add(new JLabel("Qtd:"), gbc);
        
        gbc.gridx = 1;
        JSpinner spinnerQtd = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        painelAcoes.add(spinnerQtd, gbc);

        gbc.gridx = 2;
        JButton btnAdicionarDoModelo = new JButton("Adicionar");
        painelAcoes.add(btnAdicionarDoModelo, gbc);

        // --- Seção: Outras Ações ---
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 3; // Ocupa a linha toda
        painelAcoes.add(new JSeparator(), gbc);

        gbc.gridy = 3;
        JButton btnCriarCustomizado = new JButton("Criar Inimigo Customizado...");
        painelAcoes.add(btnCriarCustomizado, gbc);

        gbc.gridy = 4;
        JButton btnRemover = new JButton("Remover Selecionado da Luta");
        painelAcoes.add(btnRemover, gbc);
        
        popup.add(painelAcoes, BorderLayout.SOUTH);

        // --- LÓGICA DOS BOTÕES ---

        btnAdicionarDoModelo.addActionListener(e -> {
            Inimigo modeloSelecionado = (Inimigo) comboModelos.getSelectedItem();
            int quantidade = (Integer) spinnerQtd.getValue();
            if (modeloSelecionado == null) return;

            Gson gson = new Gson();

            for (int i = 0; i < quantidade; i++) {
                // Clona o inimigo do modelo, como antes
                String modeloJson = gson.toJson(modeloSelecionado);
                Inimigo novoInimigo = gson.fromJson(modeloJson, Inimigo.class);
                
                String nomeBase = modeloSelecionado.getNome();
                
                // Conta quantos inimigos com o mesmo nome base já existem na luta.
                // O stream percorre a lista 'listaInimigos' e conta quantos nomes começam com o 'nomeBase'.
                long contadorExistente = listaInimigos.stream().filter(inimigo -> inimigo.getNome().startsWith(nomeBase)).count();
                                                 
                // Cria o novo nome numerado. Ex: Saibaman + " " + (0 + 1) -> "Saibaman 1"
                String novoNome = nomeBase + " " + (contadorExistente + 1);
                novoInimigo.setNome(novoNome); // Usa o novo setter que criamos

                novoInimigo.inicializarAposLoad();
                
                if (novoInimigo.getNomesTecnicas() == null || novoInimigo.getNomesTecnicas().isEmpty()) {
                    novoInimigo.setNomesTecnicas(new ArrayList<>(java.util.Arrays.asList("Soco", "Ki Blast")));
                }
                
                vincularHabilidadesParaInimigo(novoInimigo);

                listaInimigos.add(novoInimigo);
                modeloListaEmCombate.addElement(novoInimigo.getNome());
            }
            atualizarTabela();
        });
        
        btnCriarCustomizado.addActionListener(e -> {
            // Cria e exibe a nova janela de editor
            EditorDeInimigos editor = new EditorDeInimigos(this, listaTecnicas, listaTransformacoes, listaAmpliacoes);
            editor.setVisible(true); // O código para aqui até o editor ser fechado

            // Após o editor fechar, pega o resultado
            Inimigo inimigoCustomizado = editor.getInimigoCriado();

            // Se o usuário salvou (o resultado não é nulo), adiciona ao combate
            if (inimigoCustomizado != null) {
                inimigoCustomizado.inicializarAposLoad();
                vincularHabilidadesParaInimigo(inimigoCustomizado);
                
                listaInimigos.add(inimigoCustomizado);
                modeloListaEmCombate.addElement(inimigoCustomizado.getNome());
                
                atualizarTabela();
                
                salvarEstadoDoJogo(); 
            }
        });

        btnRemover.addActionListener(e -> {
            int indiceSelecionado = listaEmCombate.getSelectedIndex();
            if (indiceSelecionado != -1) {
                listaInimigos.remove(indiceSelecionado);
                modeloListaEmCombate.remove(indiceSelecionado);
                atualizarTabela();
                salvarEstadoDoJogo(); 
            }
        });

        popup.setVisible(true);
    }
	
    private void vincularHabilidadesParaInimigo(Inimigo inimigo) {
        if (inimigo.getNomesTransformacoes() != null) {
            List<Transformacao> aprendidas = new ArrayList<>();
            for (String nomeForma : inimigo.getNomesTransformacoes()) {
                listaTransformacoes.stream().filter(t -> t.getNome().equals(nomeForma)).findFirst().ifPresent(aprendidas::add);
            }
            inimigo.setTransformacoesDisponiveis(aprendidas);
        }
        if (inimigo.getNomesAmpliacoes() != null) {
            List<Ampliacao> aprendidas = new ArrayList<>();
            for (String nomeForma : inimigo.getNomesAmpliacoes()) {
                listaAmpliacoes.stream().filter(a -> a.getNome().equals(nomeForma)).findFirst().ifPresent(aprendidas::add);
            }
            inimigo.setAmpliacoesDisponiveis(aprendidas);
        }
        if (inimigo.getNomesTecnicas() != null) {
            List<Tecnica> aprendidas = new ArrayList<>();
            for (String nomeTecnica : inimigo.getNomesTecnicas()) {
                listaTecnicas.stream().filter(t -> t.getNome().equals(nomeTecnica)).findFirst().ifPresent(aprendidas::add);
            }
            inimigo.setTecnicasDisponiveis(aprendidas);
        }
        if (inimigo.getRegeneracaoPassiva() != null && inimigo.getRegeneracaoPassiva().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal hpMax = new BigDecimal(inimigo.getHpMaximoAtual());
            BigDecimal percentualCura = inimigo.getRegeneracaoPassiva();
            BigInteger curaPorTurno = hpMax.multiply(percentualCura).toBigInteger();
            
            StatusEffect efeitoRegen = new StatusEffect("Regeneração de HP", Integer.MAX_VALUE, curaPorTurno);
            log(inimigo.getNome() + " curou " + curaPorTurno + " neste turno.");
            inimigo.getEfeitosAtivos().add(efeitoRegen);
        }
    }
    
    // --- MÉTODO DE TRANSFORMAÇÃO TOTALMENTE RECONSTRUÍDO ---
    private void abrirPopupFormas() {
        JDialog popup = new JDialog(this, "Aplicar/Remover Forma", true);
        popup.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Componentes da Interface ---
        JComboBox<Object> comboPersonagem = new JComboBox<>();
        JComboBox<String> comboTipoForma = new JComboBox<>(new String[]{"Transformação", "Ampliação"});
        JComboBox<Object> comboFormaPrincipal = new JComboBox<>();
        JComboBox<Object> comboSubNivel = new JComboBox<>();
        
        comboFormaPrincipal.setRenderer(new FormaListCellRenderer());
        comboSubNivel.setRenderer(new FormaListCellRenderer());
        
        // --- Populando a lista de personagens ---
        comboPersonagem.addItem("--- Selecione um Personagem ---");
        listaJogadores.forEach(comboPersonagem::addItem);
        listaInimigos.forEach(comboPersonagem::addItem);
        
        if (!listaDeTurnos.isEmpty()) {
        	com.Vixus.Library.Personagem personagemAtivo = getPersonagemAtivo();
            
            // Como o ComboBox contém os próprios objetos, a seleção é direta
            comboPersonagem.setSelectedItem(personagemAtivo);
        }

        // --- Listeners para os ComboBoxes em cascata ---
        ActionListener atualizadorDeListas = e -> {
        	DefaultComboBoxModel<Object> modelForma = (DefaultComboBoxModel<Object>) comboFormaPrincipal.getModel();
            comboFormaPrincipal.removeAllItems();
            Object p = comboPersonagem.getSelectedItem();
            String tipoSelecionado = (String) comboTipoForma.getSelectedItem();
            
            if (p instanceof Jogador j) {
                if ("Transformação".equals(tipoSelecionado)) {
                    j.getTransformacoesDisponiveis().forEach(modelForma::addElement);
                } else {
                    j.getAmpliacoesDisponiveis().forEach(modelForma::addElement);
                }
            } else if (p instanceof Inimigo i) {
                if ("Transformação".equals(tipoSelecionado)) {
                    i.getTransformacoesDisponiveis().forEach(modelForma::addElement);
                } else {
                    i.getAmpliacoesDisponiveis().forEach(modelForma::addElement);
                }
            }
        };
        
        comboPersonagem.addActionListener(atualizadorDeListas);
        comboTipoForma.addActionListener(atualizadorDeListas);

        comboFormaPrincipal.addActionListener(e -> {
            DefaultComboBoxModel<Object> modelSub = (DefaultComboBoxModel<Object>) comboSubNivel.getModel();
            modelSub.removeAllElements();
            
            Object personagemSelecionado = comboPersonagem.getSelectedItem();
            Object formaSelecionada = comboFormaPrincipal.getSelectedItem();

            if (personagemSelecionado instanceof Jogador j) {
                if (formaSelecionada instanceof Transformacao t) {
                    int maestria = j.getMaestriaPara(t.getNome());
                    t.getSubFormas().stream()
                        .filter(sf -> sf.getMaestriaNecessaria() <= maestria) // FILTRA PELO NÍVEL DE MAESTRIA
                        .forEach(modelSub::addElement);
                } else if (formaSelecionada instanceof Ampliacao a) {
                    int maestria = j.getMaestriaPara(a.getNome());
                    a.getGraus().stream()
                        .filter(ga -> ga.getMaestriaNecessaria() <= maestria) // FILTRA PELO NÍVEL DE MAESTRIA
                        .forEach(modelSub::addElement);
                }
            } 
            if (personagemSelecionado instanceof Inimigo i) {
                if (formaSelecionada instanceof Transformacao t) {
                    int maestria = i.getMaestriaPara(t.getNome());
                    t.getSubFormas().stream()
                        .filter(sf -> sf.getMaestriaNecessaria() <= maestria) // FILTRA PELO NÍVEL DE MAESTRIA
                        .forEach(modelSub::addElement);
                } else if (formaSelecionada instanceof Ampliacao a) {
                    int maestria = i.getMaestriaPara(a.getNome());
                    a.getGraus().stream()
                        .filter(ga -> ga.getMaestriaNecessaria() <= maestria) // FILTRA PELO NÍVEL DE MAESTRIA
                        .forEach(modelSub::addElement);
                }
            }
        });

        // --- Botões e suas Ações ---
        JButton btnConfirmar = new JButton("Transformar/Ampliar");
        btnConfirmar.addActionListener(e -> {
            Object p = comboPersonagem.getSelectedItem();
            Object subNivel = comboSubNivel.getSelectedItem();

            if (!(p instanceof Jogador || p instanceof Inimigo) || subNivel == null) {
                JOptionPane.showMessageDialog(popup, "Selecione um personagem e uma Transformação/Ampliação.");
                return;
            }

            if (p instanceof Jogador j) {
                if (subNivel instanceof SubForma sf) {
                	j.aplicarSubForma(sf); 
                	log(j.getNome() + " se transformou em " + sf.getNome());
                } else if (subNivel instanceof GrauAmpliacao ga) {
                	j.aplicarAmpliacao(ga);
                	log(j.getNome() + " ativou " + ga.getNome());
                }
            } else if (p instanceof Inimigo i) {
                if (subNivel instanceof SubForma sf) {
                	i.aplicarSubForma(sf);
                	log(i.getNome() + " se transformou em " + sf.getNome());
                } else if (subNivel instanceof GrauAmpliacao ga) {
                	i.aplicarAmpliacao(ga);
                	log(i.getNome() + " ativou " + ga.getNome());
                }
            }

            atualizarTabela();
            popup.dispose();
        });

        // --- LÓGICA DOS BOTÕES DE REMOÇÃO ---
        JButton btnDestransformar = new JButton("Destransformar");
        btnDestransformar.addActionListener(e -> {
            Object p = comboPersonagem.getSelectedItem();
            if (p instanceof Jogador j) {
                j.removerTransformacao();
                log("Transformação de " + j.getNome() + " removida.");
            } else if (p instanceof Inimigo i) {
                i.removerTransformacao();
                log("Transformação de " + i.getNome() + " removida.");
            }
            atualizarTabela();
        });

        JButton btnDesampliar = new JButton("Desampliar");
        btnDesampliar.addActionListener(e -> {
            Object p = comboPersonagem.getSelectedItem();
            if (p instanceof Jogador j) {
                j.removerAmpliacao();
                log("Ampliação de " + j.getNome() + " removida.");
            } else if (p instanceof Inimigo i) {
                i.removerAmpliacao();
                log("Ampliação de " + i.getNome() + " removida.");
            }
            atualizarTabela();
        });
        
        JButton btnSubirGrau = new JButton("Subir Grau Atual");
        btnSubirGrau.addActionListener(e -> {
            Object p = comboPersonagem.getSelectedItem();
            if (p instanceof Jogador || p instanceof Inimigo) {
                executarSubirGrau(p); // Chama o novo método auxiliar
                popup.dispose(); // Fecha a janela após a ação
            } else {
                JOptionPane.showMessageDialog(popup, "Selecione um personagem válido.");
            }
        });
        
        // --- Montagem da Interface ---
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0; popup.add(new JLabel("Personagem:"), gbc);
        gbc.gridy = 1; popup.add(comboPersonagem, gbc);
        gbc.gridy = 2; popup.add(new JLabel("Tipo de Forma:"), gbc);
        gbc.gridy = 3; popup.add(comboTipoForma, gbc);
        gbc.gridy = 4; popup.add(new JLabel("Forma Principal:"), gbc);
        gbc.gridy = 5; popup.add(comboFormaPrincipal, gbc);
        gbc.gridy = 6; popup.add(new JLabel("Nível / Grau:"), gbc);
        gbc.gridy = 7; popup.add(comboSubNivel, gbc);
        gbc.gridy = 8; popup.add(btnConfirmar, gbc);
        gbc.gridy = 10; popup.add(btnSubirGrau, gbc);
        
        JPanel painelRemover = new JPanel(new GridLayout(1, 2, 5, 0));
        painelRemover.add(btnDestransformar);
        painelRemover.add(btnDesampliar);
        gbc.gridy = 9; popup.add(painelRemover, gbc);
        
        popup.pack();
        popup.setLocationRelativeTo(this);
        popup.setVisible(true);
    }
    
    private void abrirPopupInventario() {
        JDialog dialog = new JDialog(this, "Inventário", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // --- Componentes ---
        JComboBox<Jogador> jogadorCombo = new JComboBox<>(listaJogadores.toArray(new Jogador[0]));
        JPanel painelItens = new JPanel(new GridLayout(0, 2, 5, 5));
        JScrollPane scrollPane = new JScrollPane(painelItens);

        // --- Painel para Adicionar Itens ---
        JPanel painelAdicionar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<Item> comboItens = new JComboBox<>(listaItensMestre.toArray(new Item[0]));
        JButton btnAdicionar = new JButton("Adicionar Item");
        painelAdicionar.add(new JLabel("Item para Adicionar:"));
        painelAdicionar.add(comboItens);
        painelAdicionar.add(btnAdicionar);

        // --- Ações dos Componentes ---

        // Ao trocar de jogador, chama o método para redesenhar o painel
        jogadorCombo.addActionListener(e -> {
            Jogador jogadorSelecionado = (Jogador) jogadorCombo.getSelectedItem();
            redesenharPainelDeItens(jogadorSelecionado, painelItens);
        });

        // Ao adicionar um item, atualiza o mapa e chama o método para redesenhar
        btnAdicionar.addActionListener(e -> {
            Jogador jogador = (Jogador) jogadorCombo.getSelectedItem();
            Item itemParaAdicionar = (Item) comboItens.getSelectedItem();
            if (jogador == null || itemParaAdicionar == null) return;

            Map<String, Integer> inventario = jogador.getInventario();
            if (inventario == null) {
                inventario = new HashMap<>();
                jogador.setInventario(inventario);
            }
            
            inventario.put(itemParaAdicionar.getNome(), inventario.getOrDefault(itemParaAdicionar.getNome(), 0) + 1);
            salvarEstadoDoJogo();
            redesenharPainelDeItens(jogador, painelItens); // Atualiza a tela
        });

        // --- Montagem Final ---
        dialog.add(jogadorCombo, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(painelAdicionar, BorderLayout.SOUTH);
        
        // Dispara a primeira atualização ao abrir a janela
        if (!listaJogadores.isEmpty()) {
            jogadorCombo.setSelectedIndex(0);
        } else {
            redesenharPainelDeItens(null, painelItens);
        }
        
        dialog.setVisible(true);
    }
    
    private void executarSubirGrau(Object personagem) {
        List<Ampliacao> ampliacoesDisponiveis = null;
        List<Transformacao> transformacoesDisponiveis = null;
        GrauAmpliacao grauAtual = null;
        SubForma subFormaAtual = null;

        // Pega os dados corretos, seja de um Jogador ou Inimigo
        if (personagem instanceof Jogador j) {
            grauAtual = j.getGrauAmpliacaoAtual();
            subFormaAtual = j.getSubFormaAtual();
            ampliacoesDisponiveis = j.getAmpliacoesDisponiveis();
            transformacoesDisponiveis = j.getTransformacoesDisponiveis();
        } else if (personagem instanceof Inimigo i) {
            grauAtual = i.getGrauAmpliacaoAtual();
            subFormaAtual = i.getSubFormaAtual();
            ampliacoesDisponiveis = i.getAmpliacoesDisponiveis();
            transformacoesDisponiveis = i.getTransformacoesDisponiveis();
        } else {
            return; // Sai se não for um personagem válido
        }

        // Tenta subir o grau da AMPLIAÇÃO primeiro
        if (grauAtual != null) {
            for (Ampliacao ampPai : ampliacoesDisponiveis) {
                int indiceAtual = ampPai.getGraus().indexOf(grauAtual);
                if (indiceAtual != -1 && indiceAtual < ampPai.getGraus().size() - 1) {
                    GrauAmpliacao proximoGrau = ampPai.getGraus().get(indiceAtual + 1);
                    if (personagem instanceof Jogador j) j.aplicarAmpliacao(proximoGrau);
                    else if (personagem instanceof Inimigo i) i.aplicarAmpliacao(proximoGrau);
                    
                    log(ampPai.getNome() + " se ampliou em: " + proximoGrau.getNome() + "!");
                    atualizarTabela();
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "Ampliação já está no nível máximo!");
            return;
        } 

        // Se não houver ampliação, tenta subir o nível da TRANSFORMAÇÃO
        if (subFormaAtual != null) {
            for (Transformacao transPai : transformacoesDisponiveis) {
                int indiceAtual = transPai.getSubFormas().indexOf(subFormaAtual);
                if (indiceAtual != -1 && indiceAtual < transPai.getSubFormas().size() - 1) {
                    SubForma proximaForma = transPai.getSubFormas().get(indiceAtual + 1);
                    if (personagem instanceof Jogador j) j.aplicarSubForma(proximaForma);
                    else if (personagem instanceof Inimigo i) i.aplicarSubForma(proximaForma);

                    log(transPai.getNome() + " se transformou em: " + proximaForma.getNome() + "!");
                    atualizarTabela();
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "Transformação já está no nível máximo!");
            return;
        }

        // Se nenhuma forma ou ampliação estiver ativa
        JOptionPane.showMessageDialog(this, "Nenhuma forma ou ampliação ativa para evoluir.");
    }
    
    private void redesenharPainelDeItens(Jogador jogador, JPanel painelItens) {
        painelItens.removeAll(); // Limpa o painel antes de adicionar os novos itens

        if (jogador == null) {
            painelItens.revalidate();
            painelItens.repaint();
            return;
        }

        Map<String, Integer> inventario = jogador.getInventario();
        if (inventario == null) { // Garante que o inventário não seja nulo
            inventario = new HashMap<>();
            jogador.setInventario(inventario);
        }

        // Cria um botão para cada item no inventário do jogador
        for (Map.Entry<String, Integer> entry : inventario.entrySet()) {
            String nomeItem = entry.getKey();
            int qtd = entry.getValue();

            String descricao = listaItensMestre.stream()
                .filter(item -> item.getNome().equals(nomeItem))
                .map(Item::getDescricao).findFirst().orElse("?");

            JButton itemBtn = new JButton(nomeItem + " (x" + qtd + ")");
            itemBtn.setToolTipText(descricao);

            itemBtn.addActionListener(evt -> {
                usarItem(nomeItem, jogador);
                // Após usar, simplesmente redesenha o painel novamente
                redesenharPainelDeItens(jogador, painelItens); 
            });
            painelItens.add(itemBtn);
        }
        
        painelItens.revalidate();
        painelItens.repaint();
    }
    
    private void importarTecnicas(String caminho) {
        listaTecnicas.clear(); 
        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty() || linha.startsWith("#")) continue;

                String[] partes = linha.split(";");
                if (partes.length != 7) continue; 

                String nome = partes[0].trim();
                int custo = Integer.parseInt(partes[1].trim());
                int qtdDados = Integer.parseInt(partes[2].trim());
                int lados = Integer.parseInt(partes[3].trim());
                int turnosCarga = Integer.parseInt(partes[4].trim());
                boolean imparavel = Boolean.parseBoolean(partes[5].trim());
                boolean aoe = Boolean.parseBoolean(partes[6].trim());

                listaTecnicas.add(new Tecnica(nome, custo, qtdDados, lados, turnosCarga, imparavel, aoe));
            }
        } 
        catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Erro de formato no arquivo de técnicas: " + e.getMessage());
        }
    }

    
    private void usarItem(String nomeItem, Jogador jogador) {
        Item item = listaItensMestre.stream()
            .filter(i -> i.getNome().equals(nomeItem))
            .findFirst().orElse(null);

        if (item == null) {
            log("Erro ao usar item: Função de item não encontrada");
            return;
        }

        String tipo = item.getTipoEfeito();
        // Verifica se um Androide está tentando usar um item que recupera energia
        if (jogador.isAndroide() && tipo.equals("HP & Energia")) {
            JOptionPane.showMessageDialog(this, "Androides não usam este tipo de item, pois não possuem Energia!");
            return; // Ação interrompida, o item não é consumido.
        }
        // Aplica o efeito
        String valor = item.getValor();
        
        switch (tipo) {
        case "HP": {
            BigDecimal percentual = new BigDecimal(valor);
            BigInteger hpMax = jogador.getHpMaximoAtual();
            BigInteger cura = percentual.multiply(new BigDecimal(hpMax)).toBigInteger();
            BigInteger novoHp = jogador.getHP().add(cura);
            if (novoHp.compareTo(hpMax) > 0) novoHp = hpMax;
            jogador.setHp(novoHp);
            log(jogador.getNome() + " recuperou " + formatarNumero(cura) + " de HP!");
            break;
        }
        case "Mix": {
        	BigDecimal percentual = new BigDecimal(item.getValor());
            // Cura HP
            BigInteger hpMax = jogador.getHpMaximoAtual();
            BigInteger cura = percentual.multiply(new BigDecimal(hpMax)).toBigInteger();
            jogador.setHp(jogador.getHP().add(cura).min(hpMax));
            // Recupera Energia
            BigInteger energiaMax = jogador.getEnergiaBase();
            BigInteger energiaRecuperada = percentual.multiply(new BigDecimal(energiaMax)).toBigInteger();
            jogador.setEnergia(jogador.getEnergia().add(energiaRecuperada).min(energiaMax));
            log(jogador.getNome() + " usou uma Cápsula MIX! e recuperou " + formatarNumero(cura) + " de HP e " + formatarNumero(energiaRecuperada) + " de Energia!");
            break;
        }
        case "HP_Ao_Longo_Do_Tempo": {
            String[] partes = valor.split(";");
            BigDecimal percentualTotal = new BigDecimal(partes[0]);
            int duracao = Integer.parseInt(partes[1]);
            
            BigInteger curaTotal = percentualTotal.multiply(new BigDecimal(jogador.getHpMaximoAtual())).toBigInteger();
            BigInteger curaPorTurno = curaTotal.divide(BigInteger.valueOf(duracao));
            
            StatusEffect novoEfeito = new StatusEffect("Regeneração de HP", duracao, curaPorTurno);
            jogador.getEfeitosAtivos().add(novoEfeito);
            log(jogador.getNome() + " começou a se regenerar!");
            break;
        }	
        case "Energia/Bateria":
             double percentual = Double.parseDouble(valor);
              if (jogador.isAndroide()) {
                  double bateriaRecuperada = 100.0 * percentual;
                  double novaBateria = jogador.getBateria() + bateriaRecuperada;
                   novaBateria = Math.min(novaBateria, 100.0);
                  if (novaBateria > 0) {
                    jogador.setInUltradrive(false);
                  }
                  jogador.setBateria(novaBateria);
                  log(jogador.getNome() + " recuperou " + String.format("%.1f%%", bateriaRecuperada) + " de bateria!");
              } else {
                  BigInteger energiaMax = jogador.getEnergiaBase();
                  BigInteger energiaRecuperada = new BigDecimal(energiaMax).multiply(new BigDecimal(percentual)).toBigInteger();
                  BigInteger novaEnergia = jogador.getEnergia().add(energiaRecuperada);
                  if (novaEnergia.compareTo(energiaMax) > 0) novaEnergia = energiaMax;
                  jogador.setEnergia(novaEnergia);
                  log(jogador.getNome() + " recuperou " + formatarNumero(energiaRecuperada) + " de energia!");
              }
              break;

            case "Pontos de Forma":
                BigInteger pontosRecuperados = new BigInteger(valor);
                BigInteger pontosMax = jogador.getFormaPontosBase();
                BigInteger novosPontos = jogador.getFormaPontos().add(pontosRecuperados);
                if (novosPontos.compareTo(pontosMax) > 0) novosPontos = pontosMax;
                jogador.setFormaPontos(novosPontos);
                log(jogador.getNome() + " recuperou " + formatarNumero(pontosRecuperados) + " Pontos de Forma!");
                break;
                
            case "Restauração Total":
                jogador.setHp(jogador.getHpBase());
                if (!jogador.isFormaIlimitada()) {
                    jogador.setFormaPontos(jogador.getFormaPontosBase());
                }
                if (jogador.isAndroide()) {
                    jogador.setBateria(100.0);
                    jogador.setInUltradrive(true);
                } else {
                    jogador.setEnergia(jogador.getEnergiaBase());
                }
                
                log(jogador.getNome() + " comeu uma Semente dos Deuses e recuperou todas as suas forças!");
                break;
        }
        // Decrementa a quantidade no inventário do jogador (em memória)
        Map<String, Integer> inventario = jogador.getInventario();
        int qtdAtual = inventario.getOrDefault(nomeItem, 0);
        if (qtdAtual <= 0) return;

        if (qtdAtual == 1) {
            inventario.remove(nomeItem);
        } else {
            inventario.put(nomeItem, qtdAtual - 1);
        }
        salvarEstadoDoJogo();
        atualizarTabela();
    }
    
    private void salvarEstadoDoJogo() {
        // Usa o Gson para converter os objetos Java de volta para um texto JSON formatado
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        try {
            // Cria um objeto que contém as listas a serem salvas
            PersonagensData data = new PersonagensData();
            data.jogadores = this.listaJogadores;
            data.inimigos = this.listaInimigos;

            // Escreve o JSON atualizado de volta para o arquivo, sobrescrevendo o antigo
            java.io.Writer writer = Files.newBufferedWriter(Paths.get("src/com/Vixus/Resources/personagens.json"));
            gson.toJson(data, writer);
            writer.close();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar o jogo: " + e.getMessage(), "Erro de Salvamento", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void abrirPopupFusoes() {
        JDialog popup = new JDialog(this, "Gerenciar Fusões", true);
        popup.setLayout(new BorderLayout(10, 10));
        popup.setSize(500, 400);
        popup.setLocationRelativeTo(this);

        // --- Painel de Criação de Fusão ---
        JPanel painelForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Jogador> cbJogador1 = new JComboBox<>(listaJogadores.toArray(new Jogador[0]));
        JComboBox<Jogador> cbJogador2 = new JComboBox<>(listaJogadores.toArray(new Jogador[0]));
        JComboBox<String> cbTipoFusao = new JComboBox<>(new String[]{"Majin", "Metamaru", "Potara"});
        JTextField txtNomeFusao = new JTextField();

        gbc.gridx = 0; gbc.gridy = 0; painelForm.add(new JLabel("Jogador 1:"), gbc);
        gbc.gridx = 1; painelForm.add(cbJogador1, gbc);
        gbc.gridx = 0; gbc.gridy = 1; painelForm.add(new JLabel("Jogador 2:"), gbc);
        gbc.gridx = 1; painelForm.add(cbJogador2, gbc);
        gbc.gridx = 0; gbc.gridy = 2; painelForm.add(new JLabel("Tipo de Fusão:"), gbc);
        gbc.gridx = 1; painelForm.add(cbTipoFusao, gbc);
        gbc.gridx = 0; gbc.gridy = 3; painelForm.add(new JLabel("Nome da Fusão:"), gbc);
        gbc.gridx = 1; painelForm.add(txtNomeFusao, gbc);

        JButton btnFundir = new JButton("Fundir");
        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2; painelForm.add(btnFundir, gbc);

        // --- Painel para Desfazer Fusão ---
        JPanel painelDesfundir = new JPanel(new BorderLayout());
        DefaultListModel<String> modeloFusoes = new DefaultListModel<>();
        fusoesAtivas.keySet().forEach(modeloFusoes::addElement);
        JList<String> listaFusoes = new JList<>(modeloFusoes);
        
        JButton btnDesfundir = new JButton("Desfazer Fusão Selecionada");
        painelDesfundir.add(new JLabel("Fusões Ativas:"), BorderLayout.NORTH);
        painelDesfundir.add(new JScrollPane(listaFusoes), BorderLayout.CENTER);
        painelDesfundir.add(btnDesfundir, BorderLayout.SOUTH);

        // --- Ações dos Botões ---
        btnFundir.addActionListener(e -> {
            Jogador j1 = (Jogador) cbJogador1.getSelectedItem();
            Jogador j2 = (Jogador) cbJogador2.getSelectedItem();
            String tipo = (String) cbTipoFusao.getSelectedItem();
            String nomeFusao = txtNomeFusao.getText().trim();

            if (j1 == null || j2 == null || j1.equals(j2)) {
                JOptionPane.showMessageDialog(popup, "Escolha dois jogadores diferentes.");
                return;
            }
            if (nomeFusao.isEmpty()) {
                JOptionPane.showMessageDialog(popup, "Digite um nome para a fusão.");
                return;
            }
            
            realizarFusao(j1, j2, tipo, nomeFusao);
            popup.dispose(); // Fecha a janela após a ação
        });

        btnDesfundir.addActionListener(e -> {
            String fusaoSelecionada = listaFusoes.getSelectedValue();
            if (fusaoSelecionada != null) {
                desfazerFusao(fusaoSelecionada);
                popup.dispose(); // Fecha a janela após a ação
            } else {
                JOptionPane.showMessageDialog(popup, "Selecione uma fusão para desfazer.");
            }
        });

        popup.add(painelForm, BorderLayout.NORTH);
        popup.add(painelDesfundir, BorderLayout.CENTER);
        popup.setVisible(true);
    }

    private void realizarFusao(Jogador j1, Jogador j2, String tipo, String nomeFusao) {
        Jogador comandante = j1;
        BigInteger hp, atk, energia, velocidade, pontosdeforma;

        // Lógica de cálculo dos status
        switch (tipo.toLowerCase()) {
            case "majin":
                hp = j1.getHP().add(j2.getHP());
                atk = j1.getAtk().add(j2.getAtk());
                energia = comandante.getEnergia();
                velocidade = j1.getVelocidade().add(j2.getVelocidade());
                pontosdeforma = j1.getFormaPontos().add(j2.getFormaPontos());
                break;
            case "metamaru":
                hp = j1.getHP().add(j2.getHP()).multiply(BigInteger.valueOf(25));
                atk = j1.getAtk().add(j2.getAtk()).multiply(BigInteger.valueOf(25));
                energia = j1.getEnergia().add(j2.getEnergia());
                velocidade = j1.getVelocidade().add(j2.getVelocidade()).multiply(BigInteger.valueOf(25));
                pontosdeforma = j1.getFormaPontos().add(j2.getFormaPontos());
                break;
            case "potara":
                hp = j1.getHP().multiply(j2.getHP());
                atk = j1.getAtk().multiply(j2.getAtk());
                energia = j1.getEnergia().add(j2.getEnergia()).multiply(BigInteger.valueOf(2));
                velocidade = j1.getVelocidade().multiply(j2.getVelocidade());
                pontosdeforma = j1.getFormaPontos().add(j2.getFormaPontos());
                break;
            default:
                JOptionPane.showMessageDialog(this, "Tipo de fusão inválido.");
                return;
        }

        Jogador fusao = new Jogador(nomeFusao, hp, atk, energia, velocidade, pontosdeforma,
                                    comandante.getDadoP(), comandante.getDadoC(), comandante.getDadoI(),
                                    comandante.getDadoA(), comandante.getDadoL());
        fusao.inicializarAposLoad();
        fusao.setTransformacoesDisponiveis(comandante.getTransformacoesDisponiveis());
        fusao.setAmpliacoesDisponiveis(comandante.getAmpliacoesDisponiveis());
        
        java.util.Set<String> nomesUnicos = new java.util.HashSet<>();
        if (j1.getNomesTecnicas() != null) {
            nomesUnicos.addAll(j1.getNomesTecnicas());
        }
        if (j2.getNomesTecnicas() != null) {
            nomesUnicos.addAll(j2.getNomesTecnicas());
        }
        
        java.util.List<String> todasAsTecnicas = new java.util.ArrayList<>(nomesUnicos);
        fusao.setNomesTecnicas(todasAsTecnicas); // Usa o novo setter que criamos

        java.util.List<Tecnica> tecnicasDaFusao = new java.util.ArrayList<>();
        for (String nomeTecnica : todasAsTecnicas) {
            listaTecnicas.stream()
                .filter(t -> t.getNome().equals(nomeTecnica))
                .findFirst()
                .ifPresent(tecnicasDaFusao::add);
        }
        fusao.setTecnicasDisponiveis(tecnicasDaFusao);

        // Guarda os dois jogadores originais em um array
        fusoesAtivas.put(nomeFusao, new Jogador[]{j1, j2});
        
        // Remove os originais da lista de jogo e adiciona a fusão
        listaJogadores.remove(j1);
        listaJogadores.remove(j2);
        listaJogadores.add(fusao);
        
        atualizarTabela();
        log(j1.getNome() + " e " + j2.getNome() + " se fundiram em " + nomeFusao + "!");
    }

    private void desfazerFusao(String nomeFusao) {
        Jogador[] jogadoresOriginais = fusoesAtivas.get(nomeFusao);
        
        if (jogadoresOriginais != null) {
            // Remove o personagem da fusão da lista
            listaJogadores.removeIf(j -> j.getNome().equals(nomeFusao));
            
            // Adiciona os dois jogadores originais de volta à lista
            listaJogadores.add(jogadoresOriginais[0]);
            listaJogadores.add(jogadoresOriginais[1]);
            
            // Remove a fusão do registro de fusões ativas
            fusoesAtivas.remove(nomeFusao);
            
            atualizarTabela();
            log("A fusão " + nomeFusao + " foi desfeita.");
        }
    }
    
    public Jogador buscarJogadorPorNome(String nome) {
        for (Jogador j : listaJogadores) {
            if (j.getNome().equals(nome)) {
                return j;
            }
        }
        return null; // Retorna null se o loop terminar sem encontrar o jogador
    }
    
    /**
     * @param nomeTecnica O nome da técnica a ser verificada.
     * @return Uma string descrevendo o efeito, ou null se não houver.
     */
    private String getEfeitoExtraDaTecnica(String nomeTecnica) {
        switch (nomeTecnica) {
            case "Chama Maligna","SS Bombardeio Mortal","Blitz de Fótons","Sol Cruel!":
                return "Aplica Queimação (50% do atk por turno)";
            case "VENENO":
                return "Aplica Veneno";
            case "Soco de Garra","Espada do Futuro","Voleio Rápido","Raio de DEDO","Disco da Morte":
                return "Aplica Sangramento (5% do HP máximo)";
            case "Sword of Hope","Ataque de Espada Dimensional","Voleio de Perfuração":
				return "Aplica Hemorragia (20% do HP máximo)";
            default:
                return null;
        }
    }
    
    /**
     * Verifica se um inimigo foi derrotado (HP <= 0) e o remove do combate,
     * a menos que ele possua um efeito de regeneração ativo.
     * @param inimigo O inimigo que acabou de sofrer dano.
     */
    private void verificarMorteInimigo(Inimigo inimigo) {
        // 1. Verifica se o HP está zerado ou negativo
        if (inimigo.getHp().compareTo(BigInteger.ZERO) <= 0) {
            
            // 2. Verifica se o inimigo tem algum efeito de regeneração
            boolean temRegeneracao = inimigo.getEfeitosAtivos().stream()
                    .anyMatch(efeito -> efeito.getNome().equals("Regeneração de HP"));

            // 3. Decide se deve remover o inimigo
            if (temRegeneracao) {
                // Se tem regeneração, ele fica em campo, "nocauteado"
                log(inimigo.getNome() + " foi derrotado, mas pode acabar revivendo!");
            } else {
                // Se não tem regeneração, é removido permanentemente
                log(inimigo.getNome() + " foi derrotado!");
                listaInimigos.remove(inimigo);
                // A chamada a atualizarTabela() será feita no final da ação principal
            }
        }
    }
    
    private void finalizarTurno(Object personagem) {
        if (!(personagem instanceof Personagem p)) {
            return; // Sai se não for um personagem
        }

        boolean estavaCarregando = false;
        if (p instanceof Jogador j && j.isCarregando()) {
            j.reduzirTurnoDeCarga();
            if (j.getTurnosRestantesCarga() <= 0) {
                String mensagem = j.finalizarCarregamento();
                log(mensagem);
            } else {
                log(j.getNome() + " está carregando seu ataque! (" + j.getTurnosRestantesCarga() + " turnos restantes)");
            }
            estavaCarregando = true;
        } else if (p instanceof Inimigo i && i.isCarregando()) {
            i.reduzirTurnoDeCarga();
            if (i.getTurnosRestantesCarga() <= 0) {
                String mensagem = i.finalizarCarregamento();
                log(mensagem);
            }
            estavaCarregando = true;
        }

        if (!estavaCarregando) {
            aplicarEfeitosDeStatus(p);

            if (p instanceof Jogador j) {
                if (!j.isFormaIlimitada()) {
                    long custoTotal = 0;
                    if (j.getSubFormaAtual() != null) custoTotal += j.getSubFormaAtual().getCustoPorTurno();
                    if (j.getGrauAmpliacaoAtual() != null) custoTotal += j.getGrauAmpliacaoAtual().getCustoPorTurno();
                    if (custoTotal > 0) j.reduzirFormaPontos(custoTotal);
                    if (j.getFormaPontos().compareTo(BigInteger.ZERO) <= 0) {
                        boolean reverteu = false;
                        if (j.getSubFormaAtual() != null) { j.removerTransformacao(); reverteu = true; }
                        if (j.getGrauAmpliacaoAtual() != null) { j.removerAmpliacao(); reverteu = true; }
                        if (reverteu) log("A energia de " + j.getNome() + " se esgotou e retornou à forma base.");
                    }
                }
                if (j.isAndroide()) {
                    if (!j.isInUltradrive() && j.getBateria() <= -50.0) {
                        j.setInUltradrive(true);
                        j.setBateria(0.0);
                        log(j.getNome() + " Inicio da UltraCarga!\nULTRADRIVE ATIVADO!");
                    } else if (j.isInUltradrive() && j.getBateria() <= j.getLimiteMeltDown()) {
                        j.setInUltradrive(false);
                        j.setBateria(0.0);
                        log(j.getNome() + " Fim da UltraCarga!\nRetornando ao modo Overdrive.");
                    }
                }
                j.atualizarAtributos();
            } else if (p instanceof Inimigo i) {
                long custoTotal = 0;
                if (i.getSubFormaAtual() != null) custoTotal += i.getSubFormaAtual().getCustoPorTurno();
                if (i.getGrauAmpliacaoAtual() != null) custoTotal += i.getGrauAmpliacaoAtual().getCustoPorTurno();
                if (custoTotal > 0) i.reduzirFormaPontos(custoTotal);
                if (i.getFormaPontos().compareTo(BigInteger.ZERO) <= 0) {
                    boolean reverteu = false;
                    if (i.getSubFormaAtual() != null) { i.removerTransformacao(); reverteu = true; }
                    if (i.getGrauAmpliacaoAtual() != null) { i.removerAmpliacao(); reverteu = true; }
                    if(reverteu) log("A energia de " + i.getNome() + " se esgotou e retornou à forma base.");
                }
                i.atualizarAtributos();
            }
        }

        // A verificação de fim de sincronia acontece para todos, no final
        for (int i = syncGroupsAtivos.size() - 1; i >= 0; i--) {
            SyncGroup group = syncGroupsAtivos.get(i);
            if (group.isDoador(p)) {
                group.getDoadores().remove(p);
                log(p.getNome() + " encerrou sua sincronia.");
                if (group.getDoadores().isEmpty()) {
                    syncGroupsAtivos.remove(i);
                    log("A sincronia com " + group.getReceptor().getNome() + " foi completamente desfeita.");
                }
            }
        }
        
        atualizarTabela();
    }
    
    private void aplicarEfeitosDeStatus(Object personagem) {
        List<StatusEffect> efeitos;
        BigInteger hpMax;
        BigInteger hpAtual;

        if (personagem instanceof Jogador j) {
            efeitos = j.getEfeitosAtivos();
            hpMax = j.getHpMaximoAtual(); 
            hpAtual = j.getHP();
        } else if (personagem instanceof Inimigo i) {
            efeitos = i.getEfeitosAtivos();
            hpMax = i.getHpMaximoAtual();
            hpAtual = i.getHp();
        } else {
            return;
        }

        // Itera sobre uma cópia da lista para poder remover itens de forma segura
        for (StatusEffect efeito : new ArrayList<>(efeitos)) {
            if (efeito.getNome().equals("Regeneração de HP")) {
                BigInteger novoHp = hpAtual.add(efeito.getValorPorTurno());
                if (novoHp.compareTo(hpMax) > 0) {
                    novoHp = hpMax;
                }
                if (personagem instanceof Jogador j) j.setHp(novoHp);
                else if (personagem instanceof Inimigo i) {
                	i.setHp(novoHp);
                    verificarMorteInimigo(i);
                }
                //define efeitos de VENENO ETC
            } else if (efeito.getNome().equals("Veneno") || efeito.getNome().equals("Sangramento") || efeito.getNome().equals("Queimação")||efeito.getNome().equals("Hemorragia")) {
                BigInteger danoDoEfeito = efeito.getValorPorTurno();
                BigInteger novoHp = hpAtual.subtract(danoDoEfeito);
                
                // Logamos o dano do efeito
                log( ( (Personagem) personagem).getNome() + " sofreu " + formatarNumero(danoDoEfeito) + 
                     " de dano de " + efeito.getNome() + "!" );

                if (personagem instanceof Jogador j) j.setHp(novoHp);
                else if (personagem instanceof Inimigo i) i.setHp(novoHp);
            }
            
            // Reduz a duração e remove se tiver acabado
            efeito.setDuracao(efeito.getDuracao() - 1);
            if (efeito.getDuracao() <= 0) {
                efeitos.remove(efeito);
            }
        }
    }
    
    public Object buscarPersonagemPorNomeComTipo(String nomeComTipo) {
        // Retorna nulo se a entrada for inválida para evitar erros.
        if (nomeComTipo == null || nomeComTipo.trim().isEmpty()) {
            return null;
        }

        // Verifica se a string termina com " (Jogador)"
        if (nomeComTipo.endsWith(" (Jogador)")) {
            // Remove o sufixo para obter o nome puro
            String nome = nomeComTipo.replace(" (Jogador)", "");
            // Reutiliza o método que já busca jogadores por nome
            return buscarJogadorPorNome(nome);

        // Senão, verifica se termina com " (Inimigo)"
        } else if (nomeComTipo.endsWith(" (Inimigo)")) {
            // Remove o sufixo para obter o nome puro
            String nome = nomeComTipo.replace(" (Inimigo)", "");
            // Percorre a lista de inimigos para encontrar o correspondente
            for (Inimigo i : listaInimigos) {
                if (i.getNome().equals(nome)) {
                    return i; // Retorna o objeto Inimigo encontrado
                }
            }
        }
        
        // Retorna nulo se o formato não corresponder ou se o personagem não for encontrado
        return null;
    }
    
    class FormaListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            // Pega o componente padrão (um JLabel) para manter a formatação original
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            String tooltip = null;

            if (value instanceof Transformacao t) {
                // Se a forma principal tiver sub-níveis, mostra os dados do primeiro nível como preview
                if (t.getSubFormas() != null && !t.getSubFormas().isEmpty()) {
                    SubForma primeiroNivel = t.getSubFormas().get(0);
                    tooltip = String.format("Nível inicial: %.1fx, Custo: %d P.F./turno",
                            primeiroNivel.getMultiplicador(), primeiroNivel.getCustoPorTurno());
                }
            } else if (value instanceof Ampliacao a) {
                if (a.getGraus() != null && !a.getGraus().isEmpty()) {
                    GrauAmpliacao primeiroGrau = a.getGraus().get(0);
                    tooltip = String.format("Nível inicial: %.1fx, Custo: %d P.F./turno",
                            primeiroGrau.getMultiplicador(), primeiroGrau.getCustoPorTurno());
                }
            } else if (value instanceof SubForma sf) {
                // Se o item já for uma subforma, mostra seus dados diretamente
                tooltip = String.format("Multiplicador: %.1fx, Custo: %d P.F./turno",
                        sf.getMultiplicador(), sf.getCustoPorTurno());
            } else if (value instanceof GrauAmpliacao ga) {
                // Se o item já for um grau, mostra seus dados diretamente
                tooltip = String.format("Multiplicador: %.1fx, Custo: %d P.F./turno",
                        ga.getMultiplicador(), ga.getCustoPorTurno());
            }

            // Define a tooltip no componente que desenha o item da lista
            list.setToolTipText(tooltip);
            
            return this;
        }
    }
    
    private Color getProximaCorSincronia() {
        Color cor = coresDisponiveisParaSincronia.get(proximaCorIndex);
        proximaCorIndex++;
        if (proximaCorIndex >= coresDisponiveisParaSincronia.size()) {
            proximaCorIndex = 0; // Volta para o início se usar todas as cores
        }
        return cor;
    }

    public boolean isPersonagemDoador(Personagem p) {
        return syncGroupsAtivos.stream().anyMatch(group -> group.isDoador(p));
    }
    
    public boolean isPersonagemSincronizado(Personagem p) {
        return syncGroupsAtivos.stream().anyMatch(group -> group.contem(p));
    }

    /**
     * Encontra o grupo de sincronia de um personagem, se existir.
     * @return O objeto SyncGroup ou null se não estiver sincronizado.
     */
    public SyncGroup getSyncGroupDoPersonagem(Personagem p) {
        return syncGroupsAtivos.stream()
                .filter(group -> group.contem(p))
                .findFirst()
                .orElse(null);
    }

    /**
     * Encontra o grupo em que um personagem é o RECEPTOR.
     * @return O objeto SyncGroup ou null se não for um receptor.
     */
    public SyncGroup getGrupoComoReceptor(Personagem p) {
        return syncGroupsAtivos.stream()
                .filter(group -> group.getReceptor().equals(p))
                .findFirst()
                .orElse(null);
    }
    
    private void verificarTorpor(Personagem alvo, BigInteger danoSofrido) {
        if (alvo.isCarregando()) return; // Personagens carregando não podem sofrer torpor

        BigInteger hpMax = alvo.getHpMaximoAtual();
        BigInteger limiarDanoMassivo = new BigDecimal(hpMax).multiply(new BigDecimal("0.50")).toBigInteger();

        // Trigger 1: Dano massivo em um único golpe
        if (danoSofrido.compareTo(limiarDanoMassivo) >= 0) {
            log(alvo.getNome() + " sofreu dano massivo e ficou ATORDOADO!");
            alvo.getEfeitosAtivos().add(new StatusEffect("Atordoado", 1));
            alvo.resetDanoAcumulado(); // Zera o acúmulo para não dar stun duplo
            return;
        }

        // Trigger 2: Dano acumulado na rodada
        alvo.adicionarDanoAcumulado(danoSofrido);
        if (alvo.getDanoAcumuladoNoTurno().compareTo(alvo.getLimiarDeTorpor()) >= 0) {
            log(alvo.getNome() + " acumulou muito dano e ficou ATORDOADO!");
            alvo.getEfeitosAtivos().add(new StatusEffect("Atordoado", 1));
            alvo.resetDanoAcumulado();
        }
        
        if (alvo.getBateria() <= -100) {
        	log(alvo.getNome() + " ficou com a bateria muito baixa");
            alvo.getEfeitosAtivos().add(new StatusEffect("Atordoado", 1));
            alvo.setBateria(alvo.getBateria() + 50);
		}
    }
    
    private void executarAtaqueNormal(Personagem atacante, Personagem alvo, Tecnica t, BigInteger danoTotal, String textoInput, JDialog popup) {
        
    	if (!emClash) {
            BigDecimal velAtacante = new BigDecimal(atacante.getVelocidade());
            BigDecimal velAlvo = new BigDecimal(alvo.getVelocidade());
            BigDecimal margem = new BigDecimal("0.10");
            BigDecimal dezPorcento = velAtacante.multiply(margem);
            BigDecimal limiteInferior = velAtacante.subtract(dezPorcento);
            BigDecimal limiteSuperior = velAtacante.add(dezPorcento);
            if (velAlvo.compareTo(limiteInferior) >= 0 && velAlvo.compareTo(limiteSuperior) <= 0) {
                int escolhaClash = JOptionPane.showConfirmDialog(popup, "Deseja iniciar um CLASH?", "Oportunidade de Clash", JOptionPane.YES_NO_OPTION);
                if (escolhaClash == JOptionPane.YES_OPTION) {
                    iniciarClash(atacante, alvo);
                    popup.dispose();
                    return;
                }
            }
        }
    	
        boolean desviou = false;
        if (!t.isImparavel()) {
            int escolhaEsquiva = JOptionPane.showConfirmDialog(popup, "O alvo '" + alvo.getNome() + "' conseguiu desviar do ataque?", "Oportunidade de Esquiva", JOptionPane.YES_NO_OPTION);
            if (escolhaEsquiva == JOptionPane.YES_OPTION) {
                desviou = true;
            }
        } else {
            log("Ataque Imparável! " + alvo.getNome() + " não pode desviar!");
            
        }if (t.getTurnosParaCarregar() > 0) {
            gastarRecursos(atacante, textoInput);
            if (atacante instanceof Jogador j) j.iniciarCarregamento(t, alvo, danoTotal);
            else if (atacante instanceof Inimigo i) i.iniciarCarregamento(t, alvo, danoTotal);
            log(atacante.getNome() + " começou a carregar " + t.getNome() + "!");
            finalizarTurno(atacante);
            proximoTurno();
            popup.dispose();
            return;
        }

        if (desviou) {
            log(alvo.getNome() + " desviou do ataque!");
        } else {
            if (alvo instanceof Jogador jAlvo) jAlvo.setHp(jAlvo.getHP().subtract(danoTotal));
            else if (alvo instanceof Inimigo iAlvo) {
            	iAlvo.setHp(iAlvo.getHp().subtract(danoTotal));
                verificarMorteInimigo(iAlvo);
            }
            verificarTorpor(alvo, danoTotal);
            log("Ataque bem sucedido em " + alvo.getNome() + "! Dano: " + formatarNumero(danoTotal));
            aplicarEfeitosSecundarios(atacante, alvo, t);
        }
        
        log(atacante.getNome() + " curou " + atacante.getRegeneracaoPassiva()  + " de HP com sua regeneração passiva!");
        gastarRecursos(atacante, textoInput);
        finalizarTurno(atacante);
        proximoTurno();
        popup.dispose();
    }

    private void executarAtaqueAoE(Personagem atacante, Set<Personagem> alvos, Tecnica t, BigInteger danoTotal, String textoInput, JDialog popup) {
        if (alvos.isEmpty()) {
            JOptionPane.showMessageDialog(popup, "Nenhum alvo foi selecionado para o ataque em área!", "Ação Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // --- LÓGICA DE ESQUIVA EM ÁREA ---
        Set<Personagem> esquivadores = new HashSet<>();
        if (!t.isImparavel()) {
            AoEDodgeResolverDialog resolverDialog = new AoEDodgeResolverDialog(this, alvos);
            resolverDialog.setVisible(true);

            esquivadores = resolverDialog.getEsquivadores();
        } else {
            log("Ataque Imparável! Ninguém pode desviar!");
        }
        
        log(atacante.getNome() + " usou a técnica em área: " + t.getNome() + "!");
        
        BigInteger danoIndividual = danoTotal.divide(BigInteger.valueOf(alvos.size()));
        
        // Loop para aplicar o dano/esquiva em cada alvo
        for (Personagem alvo : alvos) {
            // 3. Verifica se o alvo atual está na lista de quem se esquivou
            if (esquivadores.contains(alvo)) {
                log(" -> " + alvo.getNome() + " desviou do ataque!");
            } else {
                // Se não desviou, aplica o dano e os efeitos
                if (alvo instanceof Jogador jAlvo) jAlvo.setHp(jAlvo.getHP().subtract(danoIndividual));
                else if (alvo instanceof Inimigo iAlvo) iAlvo.setHp(iAlvo.getHp().subtract(danoIndividual));
                
                verificarTorpor(alvo, danoIndividual);
                log(" -> " + alvo.getNome() + " foi atingido, sofrendo " + formatarNumero(danoIndividual) + " de dano.");
                aplicarEfeitosSecundarios(atacante, alvo, t);
                
                // Verifica se o inimigo foi derrotado após este ataque
                if (alvo instanceof Inimigo iAlvo) {
                    verificarMorteInimigo(iAlvo);
                }
            }
        }

        // O custo em recursos é pago apenas uma vez, após a ação
        gastarRecursos(atacante, textoInput);
        finalizarTurno(atacante);
        proximoTurno();
        popup.dispose();
    }
    
    private void abrirPopupTurno() {
    	if (mostrarApenasAtivos) {
            // Se a opção de mostrar apenas ativos estiver ligada, pula o turno de jogadores inativos
            Personagem p = getPersonagemAtivo();
            if (p instanceof Jogador && !((Jogador) p).isAtivoNaSessao()) {
                proximoTurno();
                abrirPopupTurno(); // Chama o método novamente para o próximo da fila
                return;
            }
        }
    	
        Personagem personagemAtivo = getPersonagemAtivo();
        if (personagemAtivo == null) return;

        // --- CLÁUSULAS DE GUARDA (INÍCIO) ---
        if (personagemAtivo instanceof Jogador j) j.resetDanoAcumulado();
        else if (personagemAtivo instanceof Inimigo i) i.resetDanoAcumulado();

        StatusEffect stun = personagemAtivo.getEfeitosAtivos().stream()
                                .filter(e -> e.getNome().equals("Atordoado")).findFirst().orElse(null);
        if (stun != null) {
            log(personagemAtivo.getNome() + " está atordoado e perdeu o turno!");
            personagemAtivo.getEfeitosAtivos().remove(stun);
            finalizarTurno(personagemAtivo);
            proximoTurno();
            return;
        }

        boolean estaCarregando = (personagemAtivo instanceof Jogador j) ? j.isCarregando() : ((Inimigo) personagemAtivo).isCarregando();
        if (estaCarregando) {
            finalizarTurno(personagemAtivo);
            proximoTurno();
            return;
        }
        // --- CLÁUSULAS DE GUARDA (FIM) ---

        JDialog popup = new JDialog(this, "Iniciar Turno", true);
        popup.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- DECLARAÇÃO DE TODOS OS COMPONENTES ---
        JComboBox<String> comboAtacante = new JComboBox<>();
        JComboBox<Tecnica> comboTecnica = new JComboBox<>();
        JTextField campoEnergia = new JTextField(10);
        JTextField campoDanoPrevisto = new JTextField(10);
        CardLayout cardLayoutAlvo = new CardLayout();
        JPanel painelSeletorDeAlvo = new JPanel(cardLayoutAlvo);
        Set<Personagem> alvosSelecionadosAoE = new java.util.HashSet<>();
        JComboBox<String> comboAlvoUnico = new JComboBox<>();
        JList<Personagem> listaMultiAlvo;
        DefaultListModel<Personagem> modeloListaMultiAlvo = new DefaultListModel<>();
        
        final boolean[] danoManualEditado = {false};

        // --- POPULANDO COMPONENTES INICIAIS ---
        listaJogadores.forEach(j -> comboAtacante.addItem(j.getNome() + " (Jogador)"));
        listaInimigos.forEach(i -> comboAtacante.addItem(i.getNome() + " (Inimigo)"));

        // --- CONFIGURAÇÃO DO PAINEL DE ALVOS (CARDLAYOUT) ---
        // Card 1: Alvo Único
        painelSeletorDeAlvo.add(comboAlvoUnico, "AlvoUnico");

        // Card 2: Múltiplos Alvos com Checkbox
        listaMultiAlvo = new JList<>(modeloListaMultiAlvo);
        listaMultiAlvo.setCellRenderer(new CheckboxListCellRenderer(alvosSelecionadosAoE));
        listaMultiAlvo.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listaMultiAlvo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = listaMultiAlvo.locationToIndex(e.getPoint());
                if (index != -1) {
                    Personagem p = listaMultiAlvo.getModel().getElementAt(index);
                    if (alvosSelecionadosAoE.contains(p)) alvosSelecionadosAoE.remove(p);
                    else alvosSelecionadosAoE.add(p);
                    listaMultiAlvo.repaint(listaMultiAlvo.getCellBounds(index, index));
                }
            }
        });
        JScrollPane scrollPaneMultiAlvo = new JScrollPane(listaMultiAlvo);
        scrollPaneMultiAlvo.setPreferredSize(new Dimension(200, 100)); // Tamanho preferido para evitar que estique a janela
        painelSeletorDeAlvo.add(scrollPaneMultiAlvo, "MultiAlvo");

        // Card 3: Painel Informativo para AoE do Inimigo (ainda usaremos)
        JTextField displayAlvoInimigo = new JTextField("Todos os Jogadores Ativos");
        displayAlvoInimigo.setEditable(false);
        displayAlvoInimigo.setHorizontalAlignment(JTextField.CENTER);
        painelSeletorDeAlvo.add(displayAlvoInimigo, "MultiAlvoInimigo");
        
        // --- LISTENERS ---
        campoDanoPrevisto.setEditable(true);
        campoDanoPrevisto.addFocusListener(new FocusAdapter() { @Override public void focusGained(FocusEvent e) { danoManualEditado[0] = true; } });
        comboTecnica.setRenderer(new DefaultListCellRenderer() {
                            @Override
                            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                                if (value instanceof Tecnica t) {
                                    setText(t.getNome());
                                    StringBuilder tooltip = new StringBuilder();
                                    tooltip.append("Custo: ").append(t.getCusto()).append(" | Dano: ").append(t.getMultiplicadorTexto());
                                    if (t.getTurnosParaCarregar() > 0) tooltip.append(" | Carga: ").append(t.getTurnosParaCarregar()).append(" turnos");
                                    if (t.isImparavel()) tooltip.append(" | Propriedade: Imparável");
                                    if (t.isAoE()) tooltip.append(" | Tipo: Área");
                                    String efeitoExtra = getEfeitoExtraDaTecnica(t.getNome());
                                    if (efeitoExtra != null) tooltip.append(" | Efeito: ").append(efeitoExtra);
                                    setToolTipText(tooltip.toString());
                                }
                                return this;
                            }
                        });	
        DocumentListener docListener = new DocumentListener() {
                public void update() {
                    if (!danoManualEditado[0]) {
                        atualizarDanoCalculado(comboAtacante, campoEnergia, comboTecnica, campoDanoPrevisto);
                    }
                }
                public void insertUpdate(DocumentEvent e) { update(); }
                public void removeUpdate(DocumentEvent e) { update(); }
                public void changedUpdate(DocumentEvent e) { update(); }
            };
        
            ActionListener tecnicaListener = e -> {
                Tecnica tecnicaSelecionada = (Tecnica) comboTecnica.getSelectedItem();
                if (tecnicaSelecionada != null && tecnicaSelecionada.isAoE()) {
                    cardLayoutAlvo.show(painelSeletorDeAlvo, "MultiAlvo");
                } else {
                    cardLayoutAlvo.show(painelSeletorDeAlvo, "AlvoUnico");
                }
            };
        
            ActionListener atacanteListener = e -> {
                atualizarTecnicasDoAtacante(comboAtacante, comboTecnica);
                
                Object atacanteObj = buscarPersonagemPorNomeComTipo((String) comboAtacante.getSelectedItem());
                
                // Limpa os seletores de alvo antes de repopular
                comboAlvoUnico.removeAllItems();
                modeloListaMultiAlvo.removeAllElements();
                alvosSelecionadosAoE.clear();

                listaInimigos.forEach(i -> comboAlvoUnico.addItem(i.getNome() + " (Inimigo)"));
                listaJogadores.stream()
                    .filter(j -> !j.equals(atacanteObj))
                    .forEach(j -> comboAlvoUnico.addItem(j.getNome() + " (Jogador)"));

                listaInimigos.forEach(modeloListaMultiAlvo::addElement);
                listaJogadores.stream()
                    .filter(Jogador::isAtivoNaSessao)
                    .filter(j -> !j.equals(atacanteObj)) 
                    .forEach(modeloListaMultiAlvo::addElement);
                
                // Dispara o listener da técnica para garantir que o painel de alvo correto seja exibido
                tecnicaListener.actionPerformed(e);
            };

        comboAtacante.addActionListener(atacanteListener);
        comboTecnica.addActionListener(tecnicaListener);
        campoEnergia.getDocument().addDocumentListener(docListener);
        campoDanoPrevisto.addFocusListener(new FocusAdapter() { @Override public void focusGained(FocusEvent e) { danoManualEditado[0] = true; } });

        // --- BOTÕES E AÇÕES ---
        JButton btnConfirmar = new JButton("Executar Ataque");
        JButton btnPassarTurno = new JButton("Passar Turno");
        JButton btnSincronizar = new JButton("Sincronizar Aliado");

        btnConfirmar.addActionListener(e -> {
                try {
                    String attSel = (String) comboAtacante.getSelectedItem();
                    Tecnica t = (Tecnica) comboTecnica.getSelectedItem();
                    String textoInput = campoEnergia.getText().trim();

                    if (attSel == null || t == null || textoInput.isEmpty()) {
                                    JOptionPane.showMessageDialog(popup, "Preencha todos os campos: atacante, técnica e energia/ações.");
                                    return;
                                }

                    Personagem atacante = (Personagem) buscarPersonagemPorNomeComTipo(attSel);
                    
                    if (atacante instanceof Jogador jAtt) {
                                    if (!jAtt.isAndroide()) {
                                        if (jAtt.getEnergia().compareTo(new BigInteger(textoInput)) < 0) {
                                            JOptionPane.showMessageDialog(popup, jAtt.getNome() + " não tem energia suficiente!"); return;
                                        }
                                    } else {
                                         if (jAtt.getBateria() > 0 && jAtt.getEnergiaEquivalente() < new BigInteger(textoInput).doubleValue()) {
                                             JOptionPane.showMessageDialog(popup, "Bateria insuficiente para esta quantidade de energia!"); return;
                                         }
                                    }
                                } else if (atacante instanceof Inimigo iAtt) {
                                    if (iAtt.getEnergia().compareTo(new BigInteger(textoInput)) < 0) {
                                        JOptionPane.showMessageDialog(popup, iAtt.getNome() + " não tem energia suficiente! O ataque falhou."); return;
                                    }
                                }
                    
                    BigInteger danoTotal = new BigInteger(campoDanoPrevisto.getText().trim());
                    
                    if (t.isAoE()) {
                        executarAtaqueAoE(atacante, alvosSelecionadosAoE, t, danoTotal, textoInput, popup);
                    } else {
                        String alvoSel = (String) comboAlvoUnico.getSelectedItem();
                        if (alvoSel == null) {
                            JOptionPane.showMessageDialog(popup, "Nenhum alvo selecionado.");
                            return;
                        }
                        Personagem alvo = (Personagem) buscarPersonagemPorNomeComTipo(alvoSel);
                        executarAtaqueNormal(atacante, alvo, t, danoTotal, textoInput, popup);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(popup, "Ocorreu um erro: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        
        btnPassarTurno.addActionListener(e -> {
                            String attSel = (String) comboAtacante.getSelectedItem();
                            if (attSel == null) {
                                JOptionPane.showMessageDialog(popup, "Selecione um personagem para passar o turno.");
                                return;
                            }
                            Object personagem = buscarPersonagemPorNomeComTipo(attSel);
                            if (personagem instanceof Jogador j) {
                                if (!j.isAndroide()) {
                                    BigInteger energiaMax = j.getEnergiaBase();
                                    BigInteger recuperacao = new BigDecimal(energiaMax).multiply(new BigDecimal("0.10")).toBigInteger();
                                    BigInteger novaEnergia = j.getEnergia().add(recuperacao);
                                    SyncGroup grupo = getGrupoComoReceptor(j);
                                    if (grupo == null) {
                                        j.setEnergia(novaEnergia.min(energiaMax));
                                    } else {
                                        j.setEnergia(novaEnergia);
                                    }
                                }
                                if (!j.isFormaIlimitada()) {
                                    long custoPorTurno = 0;
                                    if (j.getSubFormaAtual() != null) custoPorTurno += j.getSubFormaAtual().getCustoPorTurno();
                                    if (j.getGrauAmpliacaoAtual() != null) custoPorTurno += j.getGrauAmpliacaoAtual().getCustoPorTurno();
                                    BigInteger formaMax = j.getFormaPontosBase();
                                    BigInteger recuperacaoBase = new BigDecimal(formaMax).multiply(new BigDecimal("0.20")).toBigInteger();
                                    BigInteger recuperacaoTotal = recuperacaoBase.add(BigInteger.valueOf(custoPorTurno));
                                    BigInteger novaForma = j.getFormaPontos().add(recuperacaoTotal);
                                    j.setFormaPontos(novaForma.min(formaMax));
                                }
                                log(j.getNome() + " passou o turno para se recompor.");
                                finalizarTurno(j);
                            } else if (personagem instanceof Inimigo i) {
                                BigInteger energiaMax = i.getEnergiaBase();
                                BigInteger recuperacao = new BigDecimal(energiaMax).multiply(new BigDecimal("0.10")).toBigInteger();
                                BigInteger novaEnergia = i.getEnergia().add(recuperacao);
                                SyncGroup grupo = getGrupoComoReceptor(i);
                                if (grupo == null) {
                                    i.setEnergia(novaEnergia.min(energiaMax));
                                } else {
                                    i.setEnergia(novaEnergia);
                                }
                                long custoPorTurno = 0;
                                if (i.getSubFormaAtual() != null) custoPorTurno += i.getSubFormaAtual().getCustoPorTurno();
                                if (i.getGrauAmpliacaoAtual() != null) custoPorTurno += i.getGrauAmpliacaoAtual().getCustoPorTurno();
                                BigInteger formaMax = i.getFormaPontosBase();
                                BigInteger recuperacaoBase = new BigDecimal(formaMax).multiply(new BigDecimal("0.20")).toBigInteger();
                                BigInteger recuperacaoTotal = recuperacaoBase.add(BigInteger.valueOf(custoPorTurno));
                                BigInteger novaForma = i.getFormaPontos().add(recuperacaoTotal);
                                i.setFormaPontos(novaForma.min(formaMax));
                                log(i.getNome() + " passou o turno para se recompor.");
                                finalizarTurno(i);
                            }
                            proximoTurno();
                            popup.dispose();
                        });
        btnSincronizar.addActionListener(e -> {
                            Personagem doador = getPersonagemAtivo();
                            if (!(doador instanceof Jogador)) {
                                JOptionPane.showMessageDialog(popup, "Apenas jogadores podem realizar esta ação!", "Ação Inválida", JOptionPane.WARNING_MESSAGE);
                                return;
                            }

                            Jogador doadorJogador = (Jogador) doador;

                            // Verifica se o jogador já é um doador
                            if (isPersonagemDoador(doadorJogador)) {
                                // --- FLUXO 1: JOGADOR JÁ É UM DOADOR (ENVIA MAIS ENERGIA/PODER) ---
                                SyncGroup grupoExistente = getSyncGroupDoPersonagem(doadorJogador);
                                Jogador receptor = (Jogador) grupoExistente.getReceptor(); // O receptor é fixo
                                
                                try {
                                    // Se o doador for Androide
                                    if (doadorJogador.isAndroide()) {
                                        String bateriaStr = JOptionPane.showInputDialog(popup, "Você já está fornecendo poder para " + receptor.getNome() +
                                                                                               ".\nDigite a % de BATERIA a mais para enviar:", "Enviar Mais Energia", JOptionPane.PLAIN_MESSAGE);
                                        if (bateriaStr == null || bateriaStr.trim().isEmpty()) return;
                                        
                                        double bateriaAdicional = Double.parseDouble(bateriaStr);
                                        if (doadorJogador.getBateria() < bateriaAdicional) { JOptionPane.showMessageDialog(popup, "Bateria insuficiente!", "Erro", JOptionPane.ERROR_MESSAGE); return; }

                                        doadorJogador.setBateria(doadorJogador.getBateria() - bateriaAdicional);
                                        BigInteger energiaEquivalente = new BigInteger(String.valueOf((long)(bateriaAdicional * Jogador.PONTOS_ENERGIA_POR_BATERIA)));
                                        receptor.setEnergia(receptor.getEnergia().add(energiaEquivalente));
                                        JOptionPane.showMessageDialog(popup, doadorJogador.getNome() + " enviou mais poder (" + bateriaAdicional + "% de bateria)!");
                                    } 
                                    // Se o doador for normal
                                    else {
                                        String energiaStr = JOptionPane.showInputDialog(popup, "Você já está sincronizado com " + receptor.getNome() +
                                                                                                ".\nDigite a quantidade de energia a mais para enviar:", "Enviar Mais Energia", JOptionPane.PLAIN_MESSAGE);
                                        if (energiaStr == null || energiaStr.trim().isEmpty()) return;
                                        
                                        BigInteger energiaAdicional = new BigInteger(energiaStr);
                                        if (doadorJogador.getEnergia().compareTo(energiaAdicional) < 0) { JOptionPane.showMessageDialog(popup, "Energia insuficiente!", "Erro", JOptionPane.ERROR_MESSAGE); return; }
                                        
                                        doadorJogador.setEnergia(doadorJogador.getEnergia().subtract(energiaAdicional));
                                        receptor.setEnergia(receptor.getEnergia().add(energiaAdicional));
                                        log(doadorJogador.getNome() + " enviou mais " + formatarNumero(energiaAdicional) + " de energia!");
                                    }
                                } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(popup, "Valor numérico inválido.", "Erro", JOptionPane.ERROR_MESSAGE); return; }

                            } else {
                                // --- FLUXO 2: JOGADOR VAI INICIAR UMA NOVA SINCRONIA ---
                                if (isPersonagemSincronizado(doadorJogador)) { JOptionPane.showMessageDialog(popup, "Você está recebendo energia e não pode iniciar uma nova sincronia.", "Ação Inválida", JOptionPane.WARNING_MESSAGE); return; }
                                
                                // CORREÇÃO DO BUG DE MULTI-SYNC: Permite selecionar alvos que já são receptores.
                                List<Jogador> alvosPossiveis = listaJogadores.stream()
                                        .filter(j -> !j.equals(doadorJogador) && !j.isAndroide() && !isPersonagemDoador(j))
                                        .collect(java.util.stream.Collectors.toList());
                                if (alvosPossiveis.isEmpty()) { JOptionPane.showMessageDialog(popup, "Não há aliados disponíveis para sincronizar!", "Sem Alvos", JOptionPane.INFORMATION_MESSAGE); return; }

                                Jogador receptor = (Jogador) JOptionPane.showInputDialog(popup, "Escolha o aliado para sincronizar:", "Sincronizar - Passo 1/2",
                                        JOptionPane.QUESTION_MESSAGE, null, alvosPossiveis.toArray(), alvosPossiveis.get(0));
                                if (receptor == null) return;

                                try {
                                    BigInteger energiaTransferida = BigInteger.ZERO;
                                    // Se o doador for Androide
                                    if (doadorJogador.isAndroide()) {
                                        String bateriaStr = JOptionPane.showInputDialog(popup, "Digite a % de BATERIA para transferir para " + receptor.getNome() + ":", "Fornecer Poder - Passo 2/2", JOptionPane.PLAIN_MESSAGE);
                                        if (bateriaStr == null || bateriaStr.trim().isEmpty()) return;
                                        double bateriaDoada = Double.parseDouble(bateriaStr);
                                        if (doadorJogador.getBateria() < bateriaDoada) { JOptionPane.showMessageDialog(popup, "Bateria insuficiente!", "Erro", JOptionPane.ERROR_MESSAGE); return; }
                                        
                                        doadorJogador.setBateria(doadorJogador.getBateria() - bateriaDoada);
                                        energiaTransferida = new BigInteger(String.valueOf((long)(bateriaDoada * Jogador.PONTOS_ENERGIA_POR_BATERIA)));
                                    }
                                    // Se o doador for normal
                                    else {
                                        String energiaStr = JOptionPane.showInputDialog(popup, "Digite a quantidade de energia para transferir para " + receptor.getNome() + ":", "Sincronizar - Passo 2/2", JOptionPane.PLAIN_MESSAGE);
                                        if (energiaStr == null || energiaStr.trim().isEmpty()) return;
                                        energiaTransferida = new BigInteger(energiaStr);
                                        if (doadorJogador.getEnergia().compareTo(energiaTransferida) < 0) { JOptionPane.showMessageDialog(popup, "Energia insuficiente!", "Erro", JOptionPane.ERROR_MESSAGE); return; }
                                        doadorJogador.setEnergia(doadorJogador.getEnergia().subtract(energiaTransferida));
                                    }
                                    
                                    receptor.setEnergia(receptor.getEnergia().add(energiaTransferida));

                                    // Lógica para adicionar a um grupo existente ou criar um novo
                                    SyncGroup grupoDoReceptor = getGrupoComoReceptor(receptor);
                                    if (grupoDoReceptor != null) {
                                        grupoDoReceptor.adicionarDoador(doadorJogador);
                                        log(doadorJogador.getNome() + " agora também está sincronizado com " + receptor.getNome() + "!");
                                    } else {
                                        Color cor = getProximaCorSincronia();
                                        syncGroupsAtivos.add(new SyncGroup(receptor, doadorJogador, cor));
                                        log(doadorJogador.getNome() + " iniciou uma sincronia com " + receptor.getNome() + "!");
                                    }

                                } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(popup, "Valor numérico inválido.", "Erro", JOptionPane.ERROR_MESSAGE); return; }
                            }
                        });

        // --- MONTAGEM DA INTERFACE ---
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 0; popup.add(new JLabel("Atacante:"), gbc);
        gbc.gridx = 1; popup.add(comboAtacante, gbc);
        gbc.gridx = 0; gbc.gridy = 1; popup.add(new JLabel("Alvo(s):"), gbc);
        gbc.gridx = 1; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH; popup.add(painelSeletorDeAlvo, gbc);
        gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 2; popup.add(new JLabel("Técnica:"), gbc);
        gbc.gridx = 1; popup.add(comboTecnica, gbc);
        gbc.gridx = 0; gbc.gridy = 3; popup.add(new JLabel("Energia/Nº de Ataques:"), gbc);
        gbc.gridx = 1; popup.add(campoEnergia, gbc);
        gbc.gridx = 0; gbc.gridy = 4; popup.add(new JLabel("Dano Previsto:"), gbc);
        gbc.gridx = 1; popup.add(campoDanoPrevisto, gbc);
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2; popup.add(btnConfirmar, gbc);
        gbc.gridy = 6; popup.add(btnPassarTurno, gbc);
        gbc.gridy = 7; popup.add(btnSincronizar, gbc);
        
        // --- INICIALIZAÇÃO FINAL ---
        if (!listaDeTurnos.isEmpty()) {
            comboAtacante.setSelectedItem(personagemAtivo.getNome() + (personagemAtivo instanceof Jogador ? " (Jogador)" : " (Inimigo)"));
        }
        
        popup.pack();
        popup.setLocationRelativeTo(this);
        popup.setVisible(true);
    }



    private void aplicarEfeitosSecundarios(Personagem atacante, Personagem alvo, Tecnica t) {
        // Se o alvo já foi derrotado, não aplica efeitos
        if (alvo.getHP().compareTo(BigInteger.ZERO) <= 0) return;

        //Queimação
        if (t.getNome().equals("Chama Maligna") 
            || t.getNome().equals("SS Bombardeio Mortal") 
            || t.getNome().equals("Blitz de Fótons")
            || t.getNome().equals("Sol Cruel!")) {
            
            int duracaoDaQueimacao = 2; // 2 turnos
            BigInteger danoPorTurno = new BigDecimal(atacante.getAtk()).multiply(new BigDecimal("0.50")).toBigInteger(); // 100% do ATK

            StatusEffect efeitoQueimacao = new StatusEffect("Queimação", duracaoDaQueimacao, danoPorTurno);
            alvo.getEfeitosAtivos().add(efeitoQueimacao);
            log(alvo.getNome() + " está em Chamas! (50% do ATK do atacante por turno)");
        }
        //Sangramento
        if (t.getNome().equals("Soco de Garra") 
            || t.getNome().equals("Espada do Futuro") 
            || t.getNome().equals("Voleio Rápido")
            || t.getNome().equals("Raio de DEDO")
            || t.getNome().equals("Disco da Morte")) {

            int duracaoDoSangramento = 2; // 3 turnos
            BigInteger danoPorTurno = new BigDecimal(alvo.getHpMaximoAtual()).multiply(new BigDecimal("0.025")).toBigInteger(); // 5% da vida máxima atual

            StatusEffect efeitoSangramento = new StatusEffect("Sangramento", duracaoDoSangramento, danoPorTurno);
            alvo.getEfeitosAtivos().add(efeitoSangramento);
            log(alvo.getNome() + " está Sangrando! (2.5% da vida máxima atual por turno)");
        }
        // Hemorragia
        if (t.getNome().equals("Sword of Hope") 
                || t.getNome().equals("Ataque de Espada Dimensional") 
                || t.getNome().equals("Voleio de Perfuração")) {

                int duraçãoHemoragia = 2; // 2 turnos
                BigInteger danoPorTurno = new BigDecimal(alvo.getHpMaximoAtual()).multiply(new BigDecimal("0.10")).toBigInteger(); // 20% da vida máxima atual

                StatusEffect efeitoHemoragia = new StatusEffect("Hemorragia", duraçãoHemoragia, danoPorTurno);
                alvo.getEfeitosAtivos().add(efeitoHemoragia);
                log(alvo.getNome() + " está Sangrando muito! (10% da vida máxima atual por turno)");
            }
    }
    
    private void atualizarDanoCalculado(JComboBox<String> comboAtacante, JTextField campoEnergia,
            JComboBox<Tecnica> comboTecnica, JTextField campoDanoPrevisto) {

    	Tecnica t = (Tecnica) comboTecnica.getSelectedItem();
    	String textoInput = campoEnergia.getText().trim();
    	Object atacante = buscarPersonagemPorNomeComTipo((String) comboAtacante.getSelectedItem());

    	// Se qualquer informação essencial estiver faltando, o dano é zero.
    	if (t == null || textoInput.isEmpty() || atacante == null) {
    		campoDanoPrevisto.setText("0");
    		return;
    	}

    	try {
    		// LÓGICA ESPECIAL PARA ANDROIDE
    		if (atacante instanceof Jogador jAtt && jAtt.isAndroide()) {
    			int numeroDeAcoes = Integer.parseInt(textoInput);
    			if (numeroDeAcoes <= 0) {
    				campoDanoPrevisto.setText("0");
    				return;
    			}

    			// Pega o dano de UM ataque (o método getAtk() já retorna o valor com bônus se houver)
    			BigInteger danoDeUmaAcao = t.rolarDano(jAtt.getAtk());
    			// Multiplica o dano pela quantidade de ações inserida
    			BigInteger danoTotalPrevisto = danoDeUmaAcao.multiply(BigInteger.valueOf(numeroDeAcoes));
    			
    			campoDanoPrevisto.setText(danoTotalPrevisto.toString());
    			
	} else {
		// LÓGICA NORMAL PARA JOGADORES NÃO-ANDROIDES E INIMIGOS
		BigInteger energiaGasta = new BigInteger(textoInput);
		BigInteger custoTecnica = BigInteger.valueOf(t.getCusto());
		
		// Validações básicas
		if (energiaGasta.compareTo(BigInteger.ZERO) < 0 || custoTecnica.compareTo(BigInteger.ZERO) <= 0) {
			campoDanoPrevisto.setText("0");
			return;
		}

		// Calcula quantas vezes a técnica pode ser usada com a energia gasta
		BigInteger vezes = energiaGasta.divide(custoTecnica);
		if (vezes.compareTo(BigInteger.ZERO) <= 0) {
			campoDanoPrevisto.setText("0");
			return;
		}

		// Pega o ATK do personagem (seja Jogador ou Inimigo)
		BigInteger atkBaseParaCalculo = (atacante instanceof Jogador j) ? j.getAtk() : ((Inimigo) atacante).getAtk();

		if (atkBaseParaCalculo.compareTo(BigInteger.ZERO) > 0) {
			BigInteger danoTotal = BigInteger.ZERO;
			// Soma o dano para cada uso da técnica
			for (BigInteger i = BigInteger.ZERO; i.compareTo(vezes) < 0; i = i.add(BigInteger.ONE)) {
				danoTotal = danoTotal.add(t.rolarDano(atkBaseParaCalculo));
			}
			campoDanoPrevisto.setText(danoTotal.toString());
		} else {
			campoDanoPrevisto.setText("0");
		}
	}
    	} catch (NumberFormatException | ArithmeticException ex) {
    		// Se o input não for um número válido, o dano é zero.
    		campoDanoPrevisto.setText("0");
    	}

    }
    
    private void atualizarTecnicasDoAtacante(JComboBox<String> comboAtacante, JComboBox<Tecnica> comboTecnica) {
        DefaultComboBoxModel<Tecnica> model = (DefaultComboBoxModel<Tecnica>) comboTecnica.getModel();
        model.removeAllElements();
        
        Object atacante = buscarPersonagemPorNomeComTipo((String) comboAtacante.getSelectedItem());
        if (atacante instanceof Jogador j) {
            if (j.getTecnicasDisponiveis() != null) j.getTecnicasDisponiveis().forEach(model::addElement);
        } else if (atacante instanceof Inimigo i) {
            if (i.getTecnicasDisponiveis() != null) i.getTecnicasDisponiveis().forEach(model::addElement);
        }
    }
    
    public void recarregarDadosEAtualizarTabela() {
        importarDados();
        atualizarTabela();
        log("Dados do arquivo recarregados e tabela atualizada!");
    }
    
    private void gastarRecursos(Object personagem, String textoInput) {
        if (personagem instanceof Jogador j) {
            if (j.isAndroide()) {
                int numeroDeAcoes = Integer.parseInt(textoInput);
                if (j.isInUltradrive()) j.setBateria(j.getBateria() - (numeroDeAcoes * 2.0));
                else if (j.getBateria() <= 0) j.setBateria(j.getBateria() - (numeroDeAcoes * 1.0));
                else {
                    BigInteger energiaGastaEquivalente = new BigInteger(textoInput);
                    double bateriaGasta = energiaGastaEquivalente.doubleValue() * Jogador.PONTOS_ENERGIA_POR_BATERIA;
                    j.setBateria(j.getBateria() - bateriaGasta);
                }
            } else {
                j.setEnergia(j.getEnergia().subtract(new BigInteger(textoInput)));
            }
        } else if (personagem instanceof Inimigo i) {
            i.setEnergia(i.getEnergia().subtract(new BigInteger(textoInput)));
        }
    }
    
    public static void main(String[] args) {
    	// Esta linha DEVE ser a primeira a ser executada para que o tema
        // seja aplicado a todos os componentes que vierem depois.
    	//com.formdev.flatlaf.FlatDarkLaf.setup(); <- modo escuro se for necessario
        // Garante que a interface gráfica seja criada e atualizada na thread de eventos da Swing (Event Dispatch Thread).
        SwingUtilities.invokeLater(() -> {
            RPGManager app = new RPGManager();
            app.setVisible(true); 
            
            if (app.janelaDeLog != null) {
                app.janelaDeLog.setVisible(true); 
            }
            
        });
    }

}
