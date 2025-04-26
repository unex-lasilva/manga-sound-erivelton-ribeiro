//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        File repositorio = new File("repositorio");
        List<ListaReproducao> listas = new ArrayList<>();

        if (!repositorio.exists()) {
            if (repositorio.mkdir()) {
                System.out.println("Repositório criado com sucesso.");
            } else {
                System.out.println("Falha ao criar o repositório.");
            }
        }

        while (true) {
            System.out.println("\nMangaSound - Menu Principal");
            System.out.println("1. Adicionar Música ao Repositório");
            System.out.println("2. Criar Lista de Reprodução");
            System.out.println("3. Editar Lista de Reprodução");
            System.out.println("4. Executar Lista de Reprodução");
            System.out.println("5. Sair");
            System.out.print("Escolha uma opção: ");

            int opcao = Integer.parseInt(scanner.nextLine());
            switch (opcao) {
                case 1 -> adicionarMusica(scanner, repositorio);
                case 2 -> criarLista(scanner, listas);
                case 3 -> editarLista(scanner, repositorio, listas);
                case 4 -> executarLista(scanner, listas);
                case 5 -> {
                    System.out.println("Saindo...");
                    return;
                }
                default -> System.out.println("Opção inválida!");
            }
        }
    }

    private static void adicionarMusica(Scanner scanner, File repositorio) {
        System.out.print("Digite o caminho do arquivo .wav: ");
        String caminho = scanner.nextLine();
        File arquivoOriginal = new File(caminho);

        if (!arquivoOriginal.exists() || !arquivoOriginal.getName().endsWith(".wav")) {
            System.out.println("Arquivo inválido!");
            return;
        }

        File destino = new File(repositorio, arquivoOriginal.getName());
        try {
            Files.copy(arquivoOriginal.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Música adicionada com sucesso ao repositório!");
        } catch (IOException e) {
            System.out.println("Erro ao copiar arquivo: " + e.getMessage());
        }
    }

    private static void criarLista(Scanner scanner, List<ListaReproducao> listas) {
        System.out.print("Digite o nome da nova lista de reprodução: ");
        String nome = scanner.nextLine();
        listas.add(new ListaReproducao(nome));
        System.out.println("Lista criada com sucesso!");
    }

    private static void editarLista(Scanner scanner, File repositorio, List<ListaReproducao> listas) {
        if (listas.isEmpty()) {
            System.out.println("Nenhuma lista criada.");
            return;
        }

        listarListas(listas);
        System.out.print("Escolha o número da lista que deseja editar: ");
        int indice = Integer.parseInt(scanner.nextLine());

        if (indice < 0 || indice >= listas.size()) {
            System.out.println("Índice inválido.");
            return;
        }

        ListaReproducao lista = listas.get(indice);
        File[] arquivos = repositorio.listFiles((dir, name) -> name.endsWith(".wav"));
        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhuma música no repositório.");
            return;
        }

        System.out.println("Músicas disponíveis no repositório:");
        for (int i = 0; i < arquivos.length; i++) {
            System.out.println(i + ": " + arquivos[i].getName());
        }

        System.out.print("Escolha o número da música: ");
        int musicaIndice = Integer.parseInt(scanner.nextLine());
        System.out.print("Informe a posição desejada na lista: ");
        int posicao = Integer.parseInt(scanner.nextLine());

        if (musicaIndice < 0 || musicaIndice >= arquivos.length) {
            System.out.println("Música inválida.");
            return;
        }

        lista.adicionar(new Musica(arquivos[musicaIndice].getName(), arquivos[musicaIndice].getPath()), posicao);
        System.out.println("Música adicionada à lista!");
    }

    private static void executarLista(Scanner scanner, List<ListaReproducao> listas) {
        if (listas.isEmpty()) {
            System.out.println("Nenhuma lista criada.");
            return;
        }

        listarListas(listas);
        System.out.print("Escolha o número da lista que deseja executar: ");
        int indice = Integer.parseInt(scanner.nextLine());

        if (indice < 0 || indice >= listas.size()) {
            System.out.println("Índice inválido.");
            return;
        }

        listas.get(indice).executar();
    }

    private static void listarListas(List<ListaReproducao> listas) {
        for (int i = 0; i < listas.size(); i++) {
            System.out.println(i + ": " + listas.get(i).getNome());
        }
    }
}

record Musica(String nome, String caminho) {}

class ListaReproducao {
    private final String nome;
    private final List<Musica> musicas;

    public ListaReproducao(String nome) {
        this.nome = nome;
        this.musicas = new LinkedList<>();
    }

    public void adicionar(Musica musica, int posicao) {
        if (posicao < 0 || posicao > musicas.size()) {
            musicas.add(musica);
        } else {
            musicas.add(posicao, musica);
        }
    }

    public String getNome() {
        return nome;
    }

    public void executar() {
        if (musicas.isEmpty()) {
            System.out.println("Lista vazia!");
            return;
        }

        int atual = 0;
        Clip clip = null;
        long startTime;
        Scanner input = new Scanner(System.in);

        while (atual < musicas.size()) {
            try {
                if (clip != null && clip.isRunning()) {
                    clip.stop();
                }

                Musica musicaAtual = musicas.get(atual);
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(musicaAtual.caminho()));
                clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
                startTime = System.currentTimeMillis();
                System.out.println("Tocando: " + musicaAtual.nome());

                boolean aguardando = true;
                while (aguardando && clip.isOpen()) {
                    System.out.println("Comandos: (p)arar, (v)oltar, (n)ext");
                    String comando = input.nextLine();
                    switch (comando) {
                        case "p", "n" -> {
                            clip.stop();
                            aguardando = false;
                        }
                        case "v" -> {
                            long tempoTocado = System.currentTimeMillis() - startTime;
                            if (tempoTocado > 10000) {
                                clip.setMicrosecondPosition(0);
                                clip.start();
                            } else if (atual > 0) {
                                atual -= 2;
                                aguardando = false;
                            }
                        }
                        default -> System.out.println("Comando inválido.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Erro ao tocar música: " + e.getMessage());
            }
            atual++;
        }
    }
}