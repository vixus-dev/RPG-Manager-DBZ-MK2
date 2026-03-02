package com.Vixus.inc;

import com.Vixus.Library.Ampliacao;
import com.Vixus.Library.Inimigo;
import com.Vixus.Library.Tecnica;
import com.Vixus.Library.Transformacao;
import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorDeInimigos extends JDialog {

    // Campo para os atributos base
    private JTextField txtAtributosBase;

    // Componentes para as habilidades
    private DefaultListModel<Tecnica> modeloTecnicasConhecidas;
    private DefaultListModel<Transformacao> modeloTransConhecidas;
    private JList<Transformacao> listaTransConhecidas;
    private DefaultListModel<Ampliacao> modeloAmpConhecidas;
    private JList<Ampliacao> listaAmpConhecidas;

    // Mapa para guardar as maestrias enquanto editamos
    private Map<String, Integer> maestriasDefinidas = new HashMap<>();

    // Objeto que será retornado se o usuário salvar
    private Inimigo inimigoCriado = null;

    public EditorDeInimigos(Frame owner, List<Tecnica> tecnicasMestre, List<Transformacao> transformacoesMestre, List<Ampliacao> ampliacoesMestre) {
        super(owner, "Criador de Inimigos Customizado", true);
        setSize(700, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // --- PAINEL DE ATRIBUTOS BASE (UNIFICADO) ---
        JPanel painelStatus = new JPanel(new BorderLayout(10, 10));
        painelStatus.setBorder(BorderFactory.createTitledBorder("Atributos Base"));
        
        JLabel labelFormato = new JLabel("Formato: Nome;HP;ATK;Velocidade;Energia;PontosDeForma");
        labelFormato.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        painelStatus.add(labelFormato, BorderLayout.NORTH);

        txtAtributosBase = new JTextField();
        txtAtributosBase.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        painelStatus.add(txtAtributosBase, BorderLayout.CENTER);
        
        add(painelStatus, BorderLayout.NORTH);

        // --- PAINEL SELETOR DE HABILIDADES COM ABAS ---
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Aba de Técnicas (não precisa de maestria, usa o seletor simples)
        tabbedPane.addTab("Técnicas", criarPainelSeletor(tecnicasMestre, new JList<>(), new JList<>(), new DefaultListModel<>(), modeloTecnicasConhecidas = new DefaultListModel<>()));

        // Abas com editor de maestria
        tabbedPane.addTab("Transformações", criarPainelComEditorMaestria(transformacoesMestre, listaTransConhecidas = new JList<>(), modeloTransConhecidas = new DefaultListModel<>()));
        tabbedPane.addTab("Ampliações", criarPainelComEditorMaestria(ampliacoesMestre, listaAmpConhecidas = new JList<>(), modeloAmpConhecidas = new DefaultListModel<>()));
        
        add(tabbedPane, BorderLayout.CENTER);

        // --- BOTÕES DE AÇÃO PRINCIPAIS ---
        JPanel painelAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = new JButton("Criar Inimigo");
        JButton btnCancelar = new JButton("Cancelar");
        painelAcoes.add(btnSalvar);
        painelAcoes.add(btnCancelar);
        add(painelAcoes, BorderLayout.SOUTH);

        // --- LÓGICA DOS BOTÕES ---
        btnSalvar.addActionListener(e -> salvarInimigo());
        btnCancelar.addActionListener(e -> dispose());
    }
    
    /**
     * Método genérico para criar um painel seletor COM editor de maestria.
     */
    private <T> JPanel criarPainelComEditorMaestria(List<T> listaMestre, JList<T> listaConhecidas, DefaultListModel<T> modeloConhecidas) {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        
        JPanel painelSeletor = criarPainelSeletor(listaMestre, new JList<>(), listaConhecidas, new DefaultListModel<>(), modeloConhecidas);
        painelPrincipal.add(painelSeletor, BorderLayout.CENTER);

        JPanel painelMaestria = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelMaestria.setBorder(BorderFactory.createTitledBorder("Editor de Maestria"));
        
        JLabel lblMaestriaAtual = new JLabel("Maestria Atual: -");
        JSpinner spinnerMaestria = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        JButton btnDefinirMaestria = new JButton("Definir");
        
        painelMaestria.add(new JLabel("Nível:"));
        painelMaestria.add(spinnerMaestria);
        painelMaestria.add(btnDefinirMaestria);
        painelMaestria.add(lblMaestriaAtual);
        
        painelPrincipal.add(painelMaestria, BorderLayout.SOUTH);

        listaConhecidas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                T selecionado = listaConhecidas.getSelectedValue();
                if (selecionado != null) {
                    int maestria = maestriasDefinidas.getOrDefault(selecionado.toString(), 0);
                    lblMaestriaAtual.setText("Maestria Atual: " + maestria);
                    spinnerMaestria.setValue(maestria);
                } else {
                    lblMaestriaAtual.setText("Maestria Atual: -");
                }
            }
        });

        btnDefinirMaestria.addActionListener(e -> {
            T selecionado = listaConhecidas.getSelectedValue();
            if (selecionado != null) {
                int novoNivel = (Integer) spinnerMaestria.getValue();
                maestriasDefinidas.put(selecionado.toString(), novoNivel);
                lblMaestriaAtual.setText("Maestria Atual: " + novoNivel);
                JOptionPane.showMessageDialog(this, "Maestria de '" + selecionado.toString() + "' definida para " + novoNivel + "!");
            }
        });

        return painelPrincipal;
    }
    
    /**
     * Método genérico que cria o componente visual de duas listas com botões para mover itens entre elas.
     */
    private <T> JPanel criarPainelSeletor(List<T> listaMestre, JList<T> listaDisponiveis, JList<T> listaConhecidas, DefaultListModel<T> modeloDisponiveis, DefaultListModel<T> modeloConhecidas) {
        JPanel painel = new JPanel(new BorderLayout(10, 0));
        
        listaMestre.forEach(modeloDisponiveis::addElement);
        listaDisponiveis.setModel(modeloDisponiveis);
        listaConhecidas.setModel(modeloConhecidas);

        JPanel painelBotoes = new JPanel(new GridLayout(2, 1, 5, 5));
        JButton btnAdicionar = new JButton(">");
        JButton btnRemover = new JButton("<");
        painelBotoes.add(btnAdicionar);
        painelBotoes.add(btnRemover);

        btnAdicionar.addActionListener(e -> moverItens(listaDisponiveis, modeloDisponiveis, modeloConhecidas));
        btnRemover.addActionListener(e -> moverItens(listaConhecidas, modeloConhecidas, modeloDisponiveis));

        painel.add(new JScrollPane(listaDisponiveis), BorderLayout.WEST);
        painel.add(painelBotoes, BorderLayout.CENTER);
        painel.add(new JScrollPane(listaConhecidas), BorderLayout.EAST);
        return painel;
    }

    /**
     * Método genérico que executa a lógica de mover os itens selecionados de uma lista para outra.
     */
    private <T> void moverItens(JList<T> origem, DefaultListModel<T> modeloOrigem, DefaultListModel<T> modeloDestino) {
        List<T> selecionadas = origem.getSelectedValuesList();
        for (T item : selecionadas) {
            modeloDestino.addElement(item);
            modeloOrigem.removeElement(item);
        }
    }

    /**
     * Ação do botão Salvar. Valida os dados, cria o objeto Inimigo e define suas habilidades.
     */
    private void salvarInimigo() {
        String dadosBase = txtAtributosBase.getText().trim();
        if (dadosBase.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O campo de atributos base não pode estar vazio.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Inimigo inimigoTemp = Inimigo.fromLinha(dadosBase);

        if (inimigoTemp == null) {
            JOptionPane.showMessageDialog(this, "Formato de texto inválido nos atributos base.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        inimigoTemp.setNomesTecnicas(getNomesDaLista(modeloTecnicasConhecidas));
        inimigoTemp.setNomesTransformacoes(getNomesDaLista(modeloTransConhecidas));
        inimigoTemp.setNomesAmpliacoes(getNomesDaLista(modeloAmpConhecidas));
        inimigoTemp.setMaestriaFormas(this.maestriasDefinidas);

        this.inimigoCriado = inimigoTemp;
        dispose();
    }

    /**
     * Método auxiliar genérico para extrair os nomes (via toString()) dos objetos em uma lista.
     */
    private List<String> getNomesDaLista(DefaultListModel<?> model) {
        List<String> nomes = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            nomes.add(model.getElementAt(i).toString());
        }
        return nomes;
    }

    /**
     * Método público chamado pelo RPGManager para obter o inimigo que foi criado.
     */
    public Inimigo getInimigoCriado() {
        return this.inimigoCriado;
    }
}