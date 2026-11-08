# Olho nas Nuvens

Jogo de entretenimento para a sala de espera da Clínica de Olhos, feito para Android TV e Fire TV. Um olho com asinhas sobe quando alguém aperta o botão de confirmar e desce com a gravidade. Cada vão entre as nuvens vale um ponto.

É um protótipo jogável para um build autorizado depois. Não foi instalado, aberto nem publicado em nenhum aparelho.

## O que este jogo não é

- Não examina os olhos, não faz diagnóstico e não promete melhora da visão.
- Não coleta conta, paciente, anúncio, analytics nem rede. O manifesto não pede permissão.
- Não é o Orlume / VisionFlow e não usa o applicationId `com.clarivisionchart.tv.direct`.
- Não traz o logo oficial da clínica. O texto **Clínica de Olhos** é marca provisória.

## Repositório e versão

Repositório canônico: [github.com/brenolima85-cell/olho-nas-nuvens](https://github.com/brenolima85-cell/olho-nas-nuvens). Branch padrão: `main`.

Antes deste protótipo, `main` era só este README, no commit `b760e40470656876eb2cf4a12a53f01d80ebce13`. Não havia tag nem `versionName`.

Esta árvore introduz a versão de aplicativo **0.1.0** (`versionCode` 1). Não há tag nem release.

## Como colaborar

1. Clone o repositório canônico e atualize `main`.
2. Crie um branch a partir de `main`. Não commite direto em `main`.
3. Abra um pull request para `main` e espere revisão antes do merge.
4. Não crie tag, não publique na Amazon Appstore e não assine um release enquanto isso não for pedido à parte.
5. Não altere o Orlume / VisionFlow neste repositório. Ele não está aqui.

```bash
git clone https://github.com/brenolima85-cell/olho-nas-nuvens.git
cd olho-nas-nuvens
git checkout main
git pull
git checkout -b minha-mudanca
```

## Identidade do aplicativo

| | |
| --- | --- |
| Nome visível | Olho nas Nuvens |
| `applicationId` / namespace | `app.olhonasnuvens.tv` |
| `minSdk` | 21 (Android 5.0) |
| `targetSdk` / `compileSdk` | 34 |
| Bytecode | Java 8, para aparelhos antigos |
| Versão | 0.1.0 (versionCode 1) |

`minSdk` 21 é uma escolha conservadora de compilação para Android TV e Fire OS antigos. O modelo do Fire Stick da clínica não é conhecido. O fato de outro app já estar instalado nesse aparelho **não** foi usado para inferir compatibilidade. Nada aqui certifica Fire OS, Fire Stick ou Android TV.

O `targetSdk` 34 também não é uma submissão à loja. Requisitos de publicação da Amazon ficam fora deste protótipo.

## Arquitetura

App nativo de uma activity, sem motor de jogo e sem AndroidX. A simulação não importa Android, então os testes de unidade rodam na JVM.

```
app/src/main/java/app/olhonasnuvens/tv/
  MainActivity.kt     vida da tela, recorde e teclas
  GameView.kt         relógio do quadro e desenho
  game/               física, pontos, fases, texto pt-BR
  input/              debounce e mapa do controle
  persist/            dois inteiros de recorde
  render/             paleta e Canvas 16:9
app/src/test/         JUnit, sem aparelho
```

Quadro lógico de 1920×1080, desenhado em escala uniforme na TV. O eixo Y cresce para baixo. A física usa passos fixos de 1/60 s. Um tranco maior que 5 passos é descartado, em vez de teletransportar o olho. Quando a activity sai de primeiro plano, o relógio zera e uma partida em andamento fica pausada até um comando de continuar.

O miolo sólido de cada nuvem é menor que o desenho (`hitboxScale` 0.62, folga lateral e no vão). Encostar só na borda fofa passa. O teto segura o olho sem encerrar a partida. O chão e o miolo da nuvem encerram.

No primeiro 1,1 s de cada voo a gravidade fica em 35%, para a partida não acabar antes do primeiro aperto. Depois disso a gravidade é a cheia. Os dois modos usam a mesma folga e a mesma abertura suave.

| | Modo fácil | Modo normal |
| --- | --- | --- |
| Ritmo | 168 px/s, constante | 230 px/s, sobe 14 a cada 2 pontos, teto 360 |
| Vão | 540 px, constante | 400 px, desce até 310 |
| Para quem | crianças, vãos largos e ritmo menor | progressão com limite |

Esses números ainda não foram jogados numa TV. São ponto de partida.

O piscar do olho dura cerca de 0,18 s a cada 3,6 s. É a pálpebra do mascote, não um flash da tela. Não há piscada branca, anúncio nem efeito estroboscópico. Uma pessoa ainda precisa confirmar isso na TV.

## Fases

- **Início:** modo fácil, modo normal e como jogar. Voltar fecha o app.
- **Como jogar:** texto sobre o início. Voltar ou o botão Voltar retorna ao início.
- **Jogando:** OK / centro / Enter / A sobe o olho. Cima e baixo do D-pad não sobem.
- **Pausado:** Continuar, tentar de novo ou voltar ao início. Acontece também sozinho se o app sai de primeiro plano. A partida não continua escondida.
- **Fim de jogo:** o foco já está em **Tentar de novo**. O OK da partida que acabou não recomeça na hora: a tela fica por 0,7 s de tempo de simulação, só enquanto está visível. Depois, um OK recomeça no mesmo modo. A outra opção volta ao início. Voltar sai sem essa espera.
- Voltar durante o jogo pausa. No pause, Voltar continua. Sair da partida é um item do menu.

O debounce descarta repetição de tecla segurada e um segundo evento em menos de 110 ms no voo (alguns controles mandam centro e Enter juntos). O menu usa outra janela, de 180 ms, para o OK do fim de jogo não ser engolido pelo último pulo.

Toque não faz parte do jogo.

## Recorde

`SharedPreferences` guarda só `best_easy` e `best_normal`. Backup do app está desligado. Não há nome, data nem outro campo.

## Marca e desenho

Paleta de céu de manhã (azul, creme, sol parado, nuvens brancas, íris verde-água). O texto importante fica em tarja clara ou painel escuro, com seta no item focado, para leitura à distância e sem depender só da cor.

Ícone e banner (320×180) foram desenhados por `tools/generate_brand_assets.py`. São placeholder. Não são o logo da clínica.

Obstáculos são bancos de nuvem arredondados. Código, nomes e desenho são originais.

## Comandos no Terminal do macOS

Estes comandos são o caminho para um build autorizado. O protótipo foi escrito sem executar o Gradle. A auditoria estática abaixo não compila e pode ser rodada à parte.

### Pré-requisitos

JDK 17 (o AGP 8.5.2 também aceita JDK 21) e Android SDK 34. O app em si continua em bytecode 8.

```bash
brew install --cask temurin@17
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
java -version

export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Se `sdkmanager` não existir, instale “Android SDK Command-line Tools” pelo Android Studio, ou baixe as command line tools em [developer.android.com/studio](https://developer.android.com/studio#command-line-tools-only) e deixe o conteúdo em `$ANDROID_HOME/cmdline-tools/latest`.

```bash
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
yes | sdkmanager --licenses

cd olho-nas-nuvens
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
```

`local.properties` não entra no Git. Python 3 basta para a auditoria. Pillow só é necessário se for regenerar ícone e banner:

```bash
python3 -m pip install pillow
python3 tools/generate_brand_assets.py
```

### Auditoria estática

Não baixa SDK, não chama Gradle e não gera APK.

```bash
python3 tools/static_audit.py
```

### Testes de unidade

Não precisa de TV. Compila o módulo de teste na JVM. **Não foi executado nesta entrega.**

```bash
./gradlew :app:testDebugUnitTest
```

O relatório, quando alguém rodar, fica em `app/build/reports/tests/testDebugUnitTest/index.html`.

Os testes cobrem gravidade, teto de queda, pulo, teto macio, chão, pausa, pontos contados uma vez, colisão perdoada, progressão e teto do modo normal, recomeço imediato, recorde por modo, debounce de repetição, mapa do controle remoto e do A do gamepad, e ida/volta do estado salvo.

### APK de debug

**Não foi executado.** Não é release, não assina para loja e não cria tag.

```bash
./gradlew :app:assembleDebug
```

Saída esperada: `app/build/outputs/apk/debug/app-debug.apk`.

### Instalação

Só com autorização explícita, cabo ou rede de depuração, e um aparelho que apareça em `adb devices`. Isto **não** substitui outro aplicativo.

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.olhonasnuvens.tv/.MainActivity
```

Para remover só este jogo:

```bash
adb uninstall app.olhonasnuvens.tv
```

## Verificações pendentes

Nenhuma destas foi feita. O modelo do Fire Stick continua desconhecido.

- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:assembleDebug` e instalação autorizada
- [ ] Abrir o jogo numa Android TV ou Fire TV real
- [ ] OK / centro do remoto, Voltar, cima e baixo do D-pad
- [ ] Botão A, Start e B de um gamepad, inclusive repetição de tecla
- [ ] Pausar ao sair do app e só continuar com um comando
- [ ] Leitura do texto e do vão a distância, em 16:9, com overscan
- [ ] Conferir se o piscar do olho não incomoda
- [ ] Trocar a marca provisória “Clínica de Olhos” quando houver logo oficial
- [ ] Afinar velocidade, vão e gravidade depois de jogar na TV
- [ ] Decidir `targetSdk` se um dia houver publicação

Não há validação visual do jogo em execução. Os PNG do ícone e do banner são arte estática, não captura de tela do app.
