package com.Vixus.inc;

import com.Vixus.Library.Jogador;
import com.Vixus.Library.PersonagensData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader; 

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class EditorDePersonagens extends JDialog {

    private final RPGManager owner;
    private final List<Jogador> listaJogadores;
    private final String caminhoArquivo = "src/com/Vixus/Resources/personagens.json";

    private JComboBox<String> seletorJogador;
    private JTextField txtHp, txtAtk, txtEnergia, txtVelocidade, txtFormaPontos, txtLimiteMeltDown;
    private JTextField txtValorFixo, txtPercentual;
    private JCheckBox chkHp, chkAtk, chkEnergia, chkVelocidade, chkFormaPontos;
    private JPanel painelAndroide;
    
    private JPanel painelMaestria;
    private JComboBox<Object> comboFormaParaTreinar;
    private JLabel lblNivelMaestriaAtual;
    private JButton btnAumentarMaestria;
    
    private JButton btnAplicarFixo;
    private JButton btnAplicarPercentual;

    public EditorDePersonagens(RPGManager owner, List<Jogador> listaJogadores) {
        super(owner, "Editor de Personagens", true);
        this.owner = owner;
        this.listaJogadores = listaJogadores;

        setSize(600, 500);
        setLocationRelativeTo(owner);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Seção 1: Seleção de Personagem ---
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Personagem:"), gbc);
        seletorJogador = new JComboBox<>();
        seletorJogador.addItem("Todos os Jogadores");
        for (Jogador j : listaJogadores) {
            seletorJogador.addItem(j.getNome());
        }
        gbc.gridx = 1; gbc.gridwidth = 2; add(seletorJogador, gbc);

        // --- Seção 2: Edição Direta de Atributos ---
        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; add(new JLabel("HP Base:"), gbc);
        txtHp = new JTextField(15);
        gbc.gridx = 1; gbc.gridwidth = 2; add(txtHp, gbc);

        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; add(new JLabel("ATK Base:"), gbc);
        txtAtk = new JTextField(15);
        gbc.gridx = 1; gbc.gridwidth = 2; add(txtAtk, gbc);
        
        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; add(new JLabel("Energia Base:"), gbc);
        txtEnergia = new JTextField(15);
        gbc.gridx = 1; gbc.gridwidth = 2; add(txtEnergia, gbc);

        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; add(new JLabel("Velocidade Base:"), gbc);
        txtVelocidade = new JTextField(15);
        gbc.gridx = 1; gbc.gridwidth = 2; add(txtVelocidade, gbc);

        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; add(new JLabel("Pontos de Forma:"), gbc);
        txtFormaPontos = new JTextField(15);
        gbc.gridx = 1; gbc.gridwidth = 2; add(txtFormaPontos, gbc);

        // --- Seção 2.5: Painel Específico para Androide ---
        painelAndroide = new JPanel(new GridBagLayout());
        txtLimiteMeltDown = new JTextField(15);
        GridBagConstraints gbcAndroid = new GridBagConstraints();
        gbcAndroid.insets = new Insets(0,0,0,0);
        gbcAndroid.fill = GridBagConstraints.HORIZONTAL;
        gbcAndroid.gridx = 0; painelAndroide.add(new JLabel("Limite MeltDown:"), gbcAndroid);
        gbcAndroid.gridx = 1; gbcAndroid.weightx = 1.0; painelAndroide.add(txtLimiteMeltDown, gbcAndroid);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 3; add(painelAndroide, gbc);
        
        // --- Seção 3: Aumento de Atributos ---
        gbc.gridy++; add(new JSeparator(), gbc);
        
        // Checkboxes para selecionar quais atributos aumentar
        chkHp = new JCheckBox("HP"); chkAtk = new JCheckBox("ATK"); chkEnergia = new JCheckBox("Energia");
        chkVelocidade = new JCheckBox("Velocidade"); chkFormaPontos = new JCheckBox("P. Forma");
        JPanel painelChecks = new JPanel(new FlowLayout(FlowLayout.CENTER));
        painelChecks.add(chkHp); painelChecks.add(chkAtk); painelChecks.add(chkEnergia);
        painelChecks.add(chkVelocidade); painelChecks.add(chkFormaPontos);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 3; add(painelChecks, gbc);
        
        // Aumento Fixo
        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; add(new JLabel("Aumentar em (+):"), gbc);
        txtValorFixo = new JTextField(10);
        gbc.gridx = 1; add(txtValorFixo, gbc);
        btnAplicarFixo = new JButton("Aplicar");
        gbc.gridx = 2; add(btnAplicarFixo, gbc);
        
        // Aumento Percentual
        gbc.gridy++; gbc.gridwidth = 1; gbc.gridx = 0; add(new JLabel("Aumentar em (%):"), gbc);
        txtPercentual = new JTextField(10);
        gbc.gridx = 1; add(txtPercentual, gbc);
        btnAplicarPercentual = new JButton("Aplicar");
        gbc.gridx = 2; add(btnAplicarPercentual, gbc);
        
        // --- Seção 4: Gerenciador de Maestria ---
        gbc.gridy++; gbc.gridwidth = 3; add(new JSeparator(), gbc);
        
        painelMaestria = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMaestria = new GridBagConstraints();
        gbcMaestria.insets = new Insets(2, 2, 2, 2);
        gbcMaestria.fill = GridBagConstraints.HORIZONTAL;

        comboFormaParaTreinar = new JComboBox<>();
        lblNivelMaestriaAtual = new JLabel("Nível Atual: -");
        btnAumentarMaestria = new JButton("+1 Maestria");
        
        gbcMaestria.gridx = 0; gbcMaestria.weightx = 0.8; painelMaestria.add(comboFormaParaTreinar, gbcMaestria);
        gbcMaestria.gridx = 1; gbcMaestria.weightx = 0.1; painelMaestria.add(lblNivelMaestriaAtual, gbcMaestria);
        gbcMaestria.gridx = 2; gbcMaestria.weightx = 0.1; painelMaestria.add(btnAumentarMaestria, gbcMaestria);
        
        gbc.gridy++; gbc.gridx = 0; add(painelMaestria, gbc);

        // --- Seção 5: Ações Finais ---
        gbc.gridy++; add(new JSeparator(), gbc);

        // --- Seção 4: Ações Finais ---
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 3; add(new JSeparator(), gbc);
        JButton btnSalvar = new JButton("Salvar Alterações e Fechar");
        JButton btnCancelar = new JButton("Cancelar");
        JPanel painelAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelAcoes.add(btnSalvar);
        painelAcoes.add(btnCancelar);
        gbc.gridy++; add(painelAcoes, gbc);

        // --- Ações dos Componentes ---
        seletorJogador.addActionListener(e -> carregarDadosDoJogador());
        btnAplicarFixo.addActionListener(e -> aplicarAumentoFixo());
        btnAplicarPercentual.addActionListener(e -> aplicarAumentoPercentual());
        btnSalvar.addActionListener(e -> salvarAlteracoes());
        btnCancelar.addActionListener(e -> dispose());
        comboFormaParaTreinar.addActionListener(e -> atualizarDisplayMaestria());
        
        btnAumentarMaestria.addActionListener(e -> {
            Jogador jogador = getJogadorSelecionado();
            Object forma = comboFormaParaTreinar.getSelectedItem();
            if (jogador == null || forma == null) return;
            
            String nomeForma = forma.toString();
            int maestriaAtual = jogador.getMaestriaPara(nomeForma);
            int novaMaestria = maestriaAtual + 1;
            
            // Garante que o mapa existe antes de usá-lo
            if (jogador.getMaestriaFormas() == null) {
                jogador.setMaestriaFormas(new HashMap<>());
            }
            
            jogador.getMaestriaFormas().put(nomeForma, novaMaestria);
            atualizarDisplayMaestria(); // Atualiza o label para mostrar o novo nível
        });
        
        // Carga inicial dos dados
        carregarDadosDoJogador();
        pack(); // Ajusta o tamanho da janela ao conteúdo
    }

    private void carregarDadosDoJogador() {
        String selecionado = (String) seletorJogador.getSelectedItem();
        boolean todosSelecionados = "Todos os Jogadores".equals(selecionado);

        // --- LÓGICA DE HABILITAÇÃO/DESABILITAÇÃO ---
        txtHp.setEnabled(!todosSelecionados);
        txtAtk.setEnabled(!todosSelecionados);
        txtVelocidade.setEnabled(!todosSelecionados);
        txtFormaPontos.setEnabled(!todosSelecionados);
        btnAplicarFixo.setEnabled(!todosSelecionados);
        btnAplicarPercentual.setEnabled(!todosSelecionados);
        painelMaestria.setVisible(!todosSelecionados);

        if (todosSelecionados) {
            limparCampos();
            painelAndroide.setVisible(false); // Esconde o painel do androide
            txtEnergia.setEnabled(false); // Desabilita energia no modo "Todos" para evitar confusão com androides
            return;
        }

        // Carrega os dados do jogador selecionado
        listaJogadores.stream()
            .filter(j -> j.getNome().equals(selecionado))
            .findFirst()
            .ifPresent(j -> {
                txtHp.setText(j.getHpBase().toString());
                txtAtk.setText(j.getAtkBase().toString());
                txtVelocidade.setText(j.getVelocidadeBase().toString());
                txtFormaPontos.setText(j.getFormaPontosBase().toString());
                
                // Lógica para o painel do Androide
                if (j.isAndroide()) {
                    txtLimiteMeltDown.setText(String.valueOf(j.getLimiteMeltDown()));
                    txtEnergia.setText("-1");
                    txtEnergia.setEnabled(false); // Energia não pode ser editada para Androides
                    painelAndroide.setVisible(true);
                } else {
                    txtEnergia.setText(j.getEnergiaBase().toString());
                    txtEnergia.setEnabled(true);
                    painelAndroide.setVisible(false);
                }
                
                comboFormaParaTreinar.removeAllItems();
                j.getTransformacoesDisponiveis().forEach(comboFormaParaTreinar::addItem);
                j.getAmpliacoesDisponiveis().forEach(comboFormaParaTreinar::addItem);

            });
        pack(); // Reajusta o tamanho da janela caso o painel do Androide apareça/desapareça
    }

    private void limparCampos() {
        txtHp.setText("");
        txtAtk.setText("");
        txtEnergia.setText("");
        txtVelocidade.setText("");
        txtFormaPontos.setText("");
        txtLimiteMeltDown.setText("");
    }
    private void aplicarAumentoFixo() {
        // Esta função só se aplica a um único jogador selecionado
        if ("Todos os Jogadores".equals(seletorJogador.getSelectedItem())) {
            JOptionPane.showMessageDialog(this, "Aumento fixo só pode ser aplicado a um jogador de cada vez.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            BigInteger valorFixo = new BigInteger(txtValorFixo.getText().trim());

            if (chkHp.isSelected()) txtHp.setText(new BigInteger(txtHp.getText()).add(valorFixo).toString());
            if (chkAtk.isSelected()) txtAtk.setText(new BigInteger(txtAtk.getText()).add(valorFixo).toString());
            if (chkVelocidade.isSelected()) txtVelocidade.setText(new BigInteger(txtVelocidade.getText()).add(valorFixo).toString());
            if (chkFormaPontos.isSelected()) txtFormaPontos.setText(new BigInteger(txtFormaPontos.getText()).add(valorFixo).toString());
            if (chkEnergia.isSelected() && new BigInteger(txtEnergia.getText()).signum() >= 0) {
                txtEnergia.setText(new BigInteger(txtEnergia.getText()).add(valorFixo).toString());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Insira um valor numérico válido para o aumento.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void aplicarAumentoPercentual() {
        if ("Todos os Jogadores".equals(seletorJogador.getSelectedItem())) {
            JOptionPane.showMessageDialog(this, "Aumento percentual só pode ser aplicado a um jogador de cada vez.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            double percentual = Double.parseDouble(txtPercentual.getText().trim());
            if (percentual <= 0) {
                JOptionPane.showMessageDialog(this, "O percentual deve ser positivo.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BigDecimal multiplicador = BigDecimal.ONE.add(BigDecimal.valueOf(percentual / 100.0));

            if (chkHp.isSelected()) txtHp.setText(new BigDecimal(txtHp.getText()).multiply(multiplicador).toBigInteger().toString());
            if (chkAtk.isSelected()) txtAtk.setText(new BigDecimal(txtAtk.getText()).multiply(multiplicador).toBigInteger().toString());
            if (chkVelocidade.isSelected()) txtVelocidade.setText(new BigDecimal(txtVelocidade.getText()).multiply(multiplicador).toBigInteger().toString());
            if (chkFormaPontos.isSelected()) txtFormaPontos.setText(new BigDecimal(txtFormaPontos.getText()).multiply(multiplicador).toBigInteger().toString());
            if (chkEnergia.isSelected() && new BigInteger(txtEnergia.getText()).signum() >= 0) {
                txtEnergia.setText(new BigDecimal(txtEnergia.getText()).multiply(multiplicador).toBigInteger().toString());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Insira valores numéricos válidos.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void atualizarDisplayMaestria() {
        Jogador jogador = getJogadorSelecionado();
        Object forma = comboFormaParaTreinar.getSelectedItem();
        if (jogador == null || forma == null) {
            lblNivelMaestriaAtual.setText("Nível Atual: -");
            return;
        }
        
        int maestria = jogador.getMaestriaPara(forma.toString());
        lblNivelMaestriaAtual.setText("Nível Atual: " + maestria);
    }

    // Método auxiliar para pegar o jogador selecionado
    private Jogador getJogadorSelecionado() {
        String nome = (String) seletorJogador.getSelectedItem();
        if (nome == null || "Todos os Jogadores".equals(nome)) return null;
        return listaJogadores.stream().filter(j -> j.getNome().equals(nome)).findFirst().orElse(null);
    }
    
    private void salvarAlteracoes() {
        try {
            // Carrega a estrutura de dados completa do JSON
            Reader reader = Files.newBufferedReader(Paths.get(caminhoArquivo));
            PersonagensData data = new Gson().fromJson(reader, PersonagensData.class);
            reader.close();

            String selecionado = (String) seletorJogador.getSelectedItem();
            boolean paraTodos = "Todos os Jogadores".equals(selecionado);

            if (paraTodos) {
                // LÓGICA PARA APLICAR AUMENTO A TODOS OS JOGADORES
                BigInteger valorFixo = new BigInteger(txtValorFixo.getText().isEmpty() ? "0" : txtValorFixo.getText());
                double percentual = Double.parseDouble(txtPercentual.getText().isEmpty() ? "0" : txtPercentual.getText());
                BigDecimal multiplicador = BigDecimal.ONE.add(BigDecimal.valueOf(percentual / 100.0));

                for (Jogador j : data.jogadores) {
                    if (chkHp.isSelected()) j.setHpBase(new BigDecimal(j.getHpBase().add(valorFixo)).multiply(multiplicador).toBigInteger());
                    if (chkAtk.isSelected()) j.setAtkBase(new BigDecimal(j.getAtkBase().add(valorFixo)).multiply(multiplicador).toBigInteger());
                    if (chkVelocidade.isSelected()) j.setVelocidadeBase(new BigDecimal(j.getVelocidadeBase().add(valorFixo)).multiply(multiplicador).toBigInteger());
                    if (chkFormaPontos.isSelected() && !j.isFormaIlimitada()) j.setFormaPontosBase(new BigDecimal(j.getFormaPontosBase().add(valorFixo)).multiply(multiplicador).toBigInteger());
                    if (chkEnergia.isSelected() && !j.isAndroide()) j.setEnergiaBase(new BigDecimal(j.getEnergiaBase().add(valorFixo)).multiply(multiplicador).toBigInteger());
                }
            } else {
                // LÓGICA PARA EDITAR UM ÚNICO JOGADOR
                for (Jogador j : data.jogadores) {
                    if (j.getNome().equals(selecionado)) {
                        j.setHpBase(new BigInteger(txtHp.getText()));
                        j.setAtkBase(new BigInteger(txtAtk.getText()));
                        j.setVelocidadeBase(new BigInteger(txtVelocidade.getText()));
                        j.setFormaPontosBase(new BigInteger(txtFormaPontos.getText()));
                        if (!j.isAndroide()) {
                            j.setEnergiaBase(new BigInteger(txtEnergia.getText()));
                        } else {
                            j.setLimiteMeltDown(Double.parseDouble(txtLimiteMeltDown.getText()));
                        }
                        break; 
                    }
                }
            }

            // Salva a estrutura de dados modificada de volta para o arquivo JSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            java.io.Writer writer = Files.newBufferedWriter(Paths.get(caminhoArquivo));
            gson.toJson(data, writer);
            writer.close();
            
            owner.recarregarDadosEAtualizarTabela(); // Avisa a janela principal
            dispose(); // Fecha o editor

        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar. Verifique se todos os campos contêm valores válidos.", "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}