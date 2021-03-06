/**
 * Created by kanari on 2016/7/28.
 */

package logic;


import javafx.beans.property.*;
import util.Pair;
import util.TaskScheduler;

import java.awt.Point;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

public class GameManager {
	private static final int N = 8;
	private PlayerState[][] gameBoard;
	private int roundCount;

	/**
	 * Helper functions
	 */
	private static PlayerState flip(PlayerState player) {
		if (player == PlayerState.NONE) return PlayerState.NONE;
		if (player == PlayerState.BLACK) return PlayerState.WHITE;
		return PlayerState.BLACK;
	}

	public AbstractPlayer getPlayer() {
		return getPlayer(getCurrentPlayer());
	}

	public AbstractPlayer getPlayer(PlayerState player) {
		return players.get(player);
	}

	/**
	 * UI events
	 */
	public interface DropPieceHandler {
		void handle(Point point, PlayerState player, List<Point> flippedPositions);
	}

	private DropPieceHandler dropPieceHandler;
	private Consumer<PlayerState> gameOverHandler;
	private Runnable newGameHandler;
	private Consumer<String> dialogHandler;
	private Consumer<String> exceptionHandler;
	private Consumer<Runnable> executeAfterAnimationHandler;

	public DropPieceHandler getDropPieceHandler() {
		return dropPieceHandler;
	}

	public void setDropPieceHandler(DropPieceHandler dropPieceHandler) {
		this.dropPieceHandler = dropPieceHandler;
	}

	public Consumer<PlayerState> getGameOverHandler() {
		return gameOverHandler;
	}

	public void setGameOverHandler(Consumer<PlayerState> gameOverHandler) {
		this.gameOverHandler = gameOverHandler;
	}

	public Runnable getNewGameHandler() {
		return newGameHandler;
	}

	public void setNewGameHandler(Runnable newGameHandler) {
		this.newGameHandler = newGameHandler;
	}

	public Consumer<String> getDialogHandler() {
		return dialogHandler;
	}

	public void setDialogHandler(Consumer<String> dialogHandler) {
		this.dialogHandler = dialogHandler;
	}

	public Consumer<String> getExceptionHandler() {
		return exceptionHandler;
	}

	public void setExceptionHandler(Consumer<String> exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	public Consumer<Runnable> getExecuteAfterAnimationHandler() {
		return executeAfterAnimationHandler;
	}

	public void setExecuteAfterAnimationHandler(Consumer<Runnable> executeAfterAnimationHandler) {
		this.executeAfterAnimationHandler = executeAfterAnimationHandler;
	}

	private class PlayerData {
		int timeLimit;
		IntegerProperty score, remainingTime;
		ObjectProperty<PlayerState> state;

		PlayerData() {
			score = new SimpleIntegerProperty(0);
			remainingTime = new SimpleIntegerProperty(0);
			state = new SimpleObjectProperty<>(PlayerState.NONE);
		}
	}

	private PlayerData p1Data, p2Data;
	private PlayerProperty<PlayerData> playerData;

	public int getP1Score() {
		return p1ScoreProperty().get();
	}

	public IntegerProperty p1ScoreProperty() {
		return p1Data.score;
	}

	public int getP2Score() {
		return p2ScoreProperty().get();
	}

	public IntegerProperty p2ScoreProperty() {
		return p2Data.score;
	}

	public int getP1RemainingTime() {
		return p1RemainingTimeProperty().get();
	}

	public IntegerProperty p1RemainingTimeProperty() {
		return p1Data.remainingTime;
	}

	public int getP2RemainingTime() {
		return p2RemainingTimeProperty().get();
	}

	public IntegerProperty p2RemainingTimeProperty() {
		return p2Data.remainingTime;
	}

	public PlayerState getP1State() {
		return p1StateProperty().get();
	}

	public ObjectProperty<PlayerState> p1StateProperty() {
		return p1Data.state;
	}

	public PlayerState getP2State() {
		return p2StateProperty().get();
	}

	public ObjectProperty<PlayerState> p2StateProperty() {
		return p2Data.state;
	}

	private ObjectProperty<PlayerState> currentPlayer;
	private BooleanProperty gameStarted;

	public PlayerState getCurrentPlayer() {
		return currentPlayer.get();
	}

	public ObjectProperty<PlayerState> currentPlayerProperty() {
		return currentPlayer;
	}

	public boolean gameStarted() {
		return gameStartedProperty().get();
	}

	public BooleanProperty gameStartedProperty() {
		return gameStarted;
	}

	/**
	 * Main game flow
	 */
	private PlayerProperty<AbstractPlayer> players = new PlayerProperty<>();
	private PlayerProperty<GameManagerInterface> interfaces = new PlayerProperty<>();

	private BooleanProperty firstRun;

	public BooleanProperty firstRunProperty() {
		return firstRun;
	}

	public void init(AbstractPlayer p1, int p1TimeLimit, AbstractPlayer p2, int p2TimeLimit) {
		players.setBlack(p1);
		players.setWhite(p2);
		interfaces.setBlack(new GameManagerInterface(this, PlayerState.BLACK));
		interfaces.setWhite(new GameManagerInterface(this, PlayerState.WHITE));
		p1.setManager(interfaces.getBlack());
		p2.setManager(interfaces.getWhite());
		currentPlayer = new SimpleObjectProperty<>(PlayerState.NONE);

		roundCount = 0;
		gameStarted = new SimpleBooleanProperty(false);
		p1Data = new PlayerData();
		p2Data = new PlayerData();
		p1Data.timeLimit = p1TimeLimit;
		p2Data.timeLimit = p2TimeLimit;
		playerData = new PlayerProperty<>(p1Data, p2Data);
		playerData.getBlack().state.set(PlayerState.BLACK);
		playerData.getWhite().state.set(PlayerState.WHITE);
		playerTimer = new PlayerProperty<>();
		remainingTimeUpdateTimer = new PlayerProperty<>();

		firstRun = new SimpleBooleanProperty(true);
	}

	private void newGame() {
		newGame(false);
	}

	private void newGame(boolean isLoadedGame) {
		++roundCount;
		moves = new ArrayList<>();
		gameBoard = new PlayerState[N][N];
		for (int i = 0; i < N; ++i)
			Arrays.fill(gameBoard[i], PlayerState.NONE);
		candidatePositions.clear();
		newGameHandler.run();

		if (!firstRun.get()) {
			players.swap();
			players.getBlack().setManager(interfaces.getBlack());
			players.getWhite().setManager(interfaces.getWhite());
			playerData.swap();
			playerData.getBlack().state.set(PlayerState.BLACK);
			playerData.getWhite().state.set(PlayerState.WHITE);
		}
		firstRun.set(false);

		dialogHandler.accept(String.format("<i>System: Game started, <b>%s</b> goes first</i>", players.getBlack().profileName));

		gameBoard[3][3] = PlayerState.WHITE;
		gameBoard[3][4] = PlayerState.BLACK;
		gameBoard[4][3] = PlayerState.BLACK;
		gameBoard[4][4] = PlayerState.WHITE;
		dropPieceHandler.handle(new Point(3, 3), PlayerState.WHITE, new ArrayList<>());
		dropPieceHandler.handle(new Point(3, 4), PlayerState.BLACK, new ArrayList<>());
		dropPieceHandler.handle(new Point(4, 3), PlayerState.BLACK, new ArrayList<>());
		dropPieceHandler.handle(new Point(4, 4), PlayerState.WHITE, new ArrayList<>());

		if (!isLoadedGame) {
			gameStarted.set(true);
			currentPlayer.set(PlayerState.BLACK);
			updateCandidatePositions();
			startTurn(PlayerState.BLACK);
			// make sure calculation does not block the thread
			TaskScheduler.singleShot(1, () -> players.getBlack().newGame(PlayerState.BLACK));
			TaskScheduler.singleShot(1, () -> players.getWhite().newGame(PlayerState.WHITE));
		}
	}

	private PlayerProperty<TimerTask> playerTimer, remainingTimeUpdateTimer;

	private void startTurn(PlayerState player) {
		assert player == currentPlayer.get();
		int timeLimit = playerData.get(player).timeLimit;
		playerTimer.set(player, TaskScheduler.singleShot(timeLimit * 1000, () -> timeOut(player)));
		long now = new Date().getTime();
		remainingTimeUpdateTimer.set(player, TaskScheduler.repeated(100, () ->
				playerData.get(player).remainingTime.set(timeLimit - (int) Math.ceil((new Date().getTime() - now) / 1000))));
	}

	private void endTurn(PlayerState player) {
		TimerTask timerTask = playerTimer.get(player);
		if (timerTask != null) timerTask.cancel();
		timerTask = remainingTimeUpdateTimer.get(player);
		if (timerTask != null) timerTask.cancel();
		playerData.get(player).remainingTime.set(0);
	}

	private void timeOut(PlayerState player) {
		endTurn(player);
		dialogHandler.accept(String.format("<i><b>%s</b> timed out</i>", players.get(player).profileName));
		if (player != getCurrentPlayer()) return;
		Point move = getPlayer(player).timeOut();
		dropPiece(move.x, move.y, player, true);
	}

	private ArrayList<Point> candidatePositions = new ArrayList<>();

	public List<Point> getCandidatePositions() {
		return Collections.unmodifiableList(candidatePositions);
	}

	private boolean isCandidatePosition(int x, int y, PlayerState player) {
		if (gameBoard[x][y] != PlayerState.NONE) return false;
		for (int dx = -1; dx <= 1; ++dx)
			for (int dy = -1; dy <= 1; ++dy) {
				if (dx == 0 && dy == 0) continue;
				boolean existsPiece = false;
				boolean shouldFlip = false;
				int tx = x, ty = y;
				while (isValid(tx + dx, ty + dy)) {
					tx += dx;
					ty += dy;
					PlayerState state = getState(tx, ty);
					if (state == player) {
						existsPiece = true;
						break;
					} else if (state == PlayerState.NONE) {
						shouldFlip = false;
						break;
					} else shouldFlip = true;
				}
				if (!existsPiece) shouldFlip = false;
				if (shouldFlip) return true;
			}
		return false;
	}

	private void updateCandidatePositions(PlayerState player) {
		candidatePositions.clear();
		for (int x = 0; x < N; ++x)
			for (int y = 0; y < N; ++y)
				if (isCandidatePosition(x, y, player))
					candidatePositions.add(new Point(x, y));
	}

	private void updateCandidatePositions() {
		updateCandidatePositions(getCurrentPlayer());
	}

	void dropPiece(int x, int y, PlayerState player, boolean isTimeout) {
		if (getCurrentPlayer() != player) return;

		endTurn(player);
		gameBoard[x][y] = player;

		Point point = new Point(x, y);
		List<Point> flippedPositions = getFlippedPositions(x, y, player);
		for (Point p : flippedPositions)
			gameBoard[p.x][p.y] = flip(gameBoard[p.x][p.y]);
		dropPieceHandler.handle(point, player, flippedPositions);
		moves.add(Pair.of(Pair.of(point, player), flippedPositions));

		Optional<PlayerState> winner = checkWinner();
		if (!winner.isPresent()) {
			// candidate positions already updated in checkWinner()
			boolean isSkipped = candidatePositions.size() == 0;
			if (isSkipped) {
				players.get(flip(player)).informOpponentMove(point, true, isTimeout);
				updateCandidatePositions(player); // should not be empty
				assert candidatePositions.size() > 0;
				startTurn(currentPlayer.get());
				TaskScheduler.singleShot(1, () -> players.get(player).informOpponentMove(null, false, false));
			} else {
				currentPlayer.set(flip(getCurrentPlayer()));
				startTurn(currentPlayer.get());
				TaskScheduler.singleShot(1, () -> players.get(flip(player)).informOpponentMove(point, false, isTimeout));
			}
		} else {
			PlayerState result = winner.get();
			// still need to inform opponent, but should prevent opponent from making a move
			players.get(flip(player)).informOpponentMove(point, true, isTimeout);
			gameOver(result);
		}
	}

	List<Point> getFlippedPositions(int x, int y, PlayerState player) {
		ArrayList<Point> flipped = new ArrayList<>();
		for (int dx = -1; dx <= 1; ++dx)
			for (int dy = -1; dy <= 1; ++dy) {
				if (dx == 0 && dy == 0) continue;
				boolean existsPiece = false;
				boolean shouldFlip = false;
				int tx = x, ty = y;
				while (isValid(tx + dx, ty + dy)) {
					tx += dx;
					ty += dy;
					PlayerState state = getState(tx, ty);
					if (state == player) {
						existsPiece = true;
						break;
					} else if (state == PlayerState.NONE) {
						shouldFlip = false;
						break;
					} else shouldFlip = true;
				}
				if (!existsPiece) shouldFlip = false;
				if (shouldFlip) {
					for (int i = x + dx, j = y + dy; i != tx || j != ty; i += dx, j += dy)
						flipped.add(new Point(i, j));
				}
			}
		return Collections.unmodifiableList(flipped);
	}

	private void gameOver(PlayerState result) {
		endTurn(PlayerState.BLACK);
		endTurn(PlayerState.WHITE);
		gameStarted.set(false);
		players.getBlack().gameOver(result == PlayerState.BLACK, result == PlayerState.NONE);
		players.getWhite().gameOver(result == PlayerState.WHITE, result == PlayerState.NONE);
		currentPlayer.set(PlayerState.NONE);
		isReady.setBlack(false);
		isReady.setWhite(false);

		executeAfterAnimationHandler.accept(() -> {
			gameOverHandler.accept(result);
			if (result != PlayerState.NONE) {
				PlayerData winner = playerData.get(result);
				winner.score.set(winner.score.get() + 1);
				dialogHandler.accept(String.format("<u>Game result: <b>%s</b> won</u>", players.get(result).profileName));
			} else {
				dialogHandler.accept("<u>Game result: <b>Draw</b></u>");
			}
		});
	}

	private Optional<PlayerState> checkWinner() {
		boolean gameOver = true;
		updateCandidatePositions();
		if (candidatePositions.size() > 0) gameOver = false;
		updateCandidatePositions(flip(getCurrentPlayer()));
		if (candidatePositions.size() > 0) gameOver = false;
		if (!gameOver) return Optional.empty();

		int blackCnt = getPieces(PlayerState.BLACK), whiteCnt = getPieces(PlayerState.WHITE);
		if (blackCnt > whiteCnt) return Optional.of(PlayerState.BLACK);
		else if (blackCnt < whiteCnt) return Optional.of(PlayerState.WHITE);
		else return Optional.of(PlayerState.NONE);
	}

	/**
	 * Player interactions
	 */
	public void forceExit(String message) {
		TaskScheduler.singleShot(50, () -> {
			endTurn(PlayerState.BLACK);
			endTurn(PlayerState.WHITE);
			players.getBlack().purge();
			players.getWhite().purge();
			exceptionHandler.accept(message);
		});
	}

	boolean canDrop(int x, int y) {
		return gameBoard[x][y] == PlayerState.NONE && candidatePositions.contains(new Point(x, y));
	}

	private boolean isValid(int x, int y) {
		return x >= 0 && x < N && y >= 0 && y < N;
	}

	public int getPieces(PlayerState player) {
		int result = 0;
		for (int i = 0; i < N; ++i)
			for (int j = 0; j < N; ++j)
				if (gameBoard[i][j] == player)
					++result;
		return result;
	}

	public PlayerState getState(int x, int y) {
		return gameBoard[x][y];
	}

	public PlayerState getState(Point point) {
		return gameBoard[point.x][point.y];
	}

	private PlayerProperty<Boolean> isReady = new PlayerProperty<>(false, false);

	boolean isReady(PlayerState player) {
		return isReady.get(player);
	}

	void ready(PlayerState player) {
		if (gameStarted.get() || isReady(player)) return;
		dialogHandler.accept(String.format("<i><b>%s</b> is ready</i>", players.get(player).profileName));
		isReady.set(player, true);
		getPlayer(flip(player)).opponentIsReady();
		if (isReady.getBlack() && isReady.getWhite()) newGame();
	}

	private ArrayList<Pair<Pair<Point, PlayerState>, List<Point>>> moves;

	boolean canUndo(PlayerState player) {
		int lastPos;
		for (lastPos = moves.size() - 1; lastPos >= 0; --lastPos)
			if (moves.get(lastPos).fst.snd == player) break;
		return lastPos >= 0;
	}

	boolean requestUndo(PlayerState player) {
		int lastPos;
		for (lastPos = moves.size() - 1; lastPos >= 0; --lastPos)
			if (moves.get(lastPos).fst.snd == player) break;
		if (lastPos < 0) return false; // can not undo

		dialogHandler.accept(String.format("<i><b>%s</b> sent a request for undoing last move</i>", players.get(player).profileName));
		boolean result = players.get(flip(player)).undoRequested();
		dialogHandler.accept(String.format("<i><b>%s</b> %s <b>%s</b>'s undo request</i>",
				players.get(flip(player)).profileName, result ? "accepted" : "refused", players.get(player).profileName));
		if (!result) return false;

		for (int i = moves.size() - 1; i >= lastPos; --i) {
			Pair<Pair<Point, PlayerState>, List<Point>> move = moves.remove(i);
			gameBoard[move.fst.fst.x][move.fst.fst.y] = PlayerState.NONE;
			for (Point point : move.snd)
				gameBoard[point.x][point.y] = flip(gameBoard[point.x][point.y]);
			dropPieceHandler.handle(move.fst.fst, PlayerState.NONE, move.snd);
		}
		updateCandidatePositions(player);
		return true;
	}

	boolean requestDraw(PlayerState player) {
		dialogHandler.accept(String.format("<i><b>%s</b> sent a request for declaring a draw</i>", players.get(player).profileName));
		boolean result = players.get(flip(player)).drawRequested();
		dialogHandler.accept(String.format("<i><b>%s</b> %s <b>%s</b>'s draw request</i>",
				players.get(flip(player)).profileName, result ? "accepted" : "refused", players.get(player).profileName));
		if (!result) return false;

		gameOver(PlayerState.NONE);
		return true;
	}

	boolean requestSurrender(PlayerState player) {
		dialogHandler.accept(String.format("<i><b>%s</b> sent a request for declaring defeat</i>", players.get(player).profileName));
		boolean result = players.get(flip(player)).surrenderRequested();
		dialogHandler.accept(String.format("<i><b>%s</b> %s <b>%s</b>'s surrender request</i>",
				players.get(flip(player)).profileName, result ? "accepted" : "refused", players.get(player).profileName));
		if (!result) return false;

		gameOver(flip(player));
		return true;
	}

	boolean requestExit(PlayerState player) {
		dialogHandler.accept(String.format("<i><b>%s</b> sent a request for quitting this match</i>", players.get(player).profileName));
		boolean result = players.get(flip(player)).exitRequested();
		dialogHandler.accept(String.format("<i><b>%s</b> %s <b>%s</b>'s exit request</i>",
				players.get(flip(player)).profileName, result ? "accepted" : "refused", players.get(player).profileName));
		if (!result) return false;

		forceExit(""); // does not should exception dialog
		return true;
	}

	void sendChat(PlayerState player, String message) {
		players.get(flip(player)).receivedChat(message);
		dialogHandler.accept(String.format("<b>%s: </b>%s", players.get(player).profileName, message));
	}

	public boolean saveGame(String filename) {
//		if (!gameStarted()) return false;
		try {
			PrintWriter writer = new PrintWriter(new FileOutputStream(filename));
			writer.println(moves.size());
			for (int i = 0; i < moves.size(); ++i) {
				Pair<Pair<Point, PlayerState>, List<Point>> move = moves.get(i);
				writer.println(move.fst.fst.x + " " + move.fst.fst.y + " " + move.fst.snd);
			}
			writer.close();
		} catch (FileNotFoundException e) {
			return false;
		}
		return true;
	}

	public boolean loadGame(String filename) {
		if (gameStarted()) return false;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			int movesCnt = Integer.parseInt(reader.readLine());
			ArrayList<Pair<Point, PlayerState>> moves = new ArrayList<>();
			for (int i = 0; i < movesCnt; ++i) {
				String[] parts = reader.readLine().split(" ");
				int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]);
				PlayerState player = PlayerState.valueOf(parts[2]);
				moves.add(Pair.of(new Point(x, y), player));
			}

			newGame(true);
			for (int i = 0; i < moves.size() - 1; ++i) {
				Pair<Point, PlayerState> move = moves.get(i);
				int x = move.fst.x, y = move.fst.y;
				PlayerState player = move.snd;

				gameBoard[x][y] = player;
				List<Point> flippedPositions = getFlippedPositions(x, y, player);
				for (Point p : flippedPositions)
					gameBoard[p.x][p.y] = flip(gameBoard[p.x][p.y]);
				dropPieceHandler.handle(move.fst, player, flippedPositions);
				this.moves.add(Pair.of(Pair.of(move.fst, player), flippedPositions));
			}
			executeAfterAnimationHandler.accept(() -> {
				Pair<Point, PlayerState> move = moves.get(moves.size() - 1);
				currentPlayer.set(move.snd);
				gameStarted.set(true);
				dropPiece(move.fst.x, move.fst.y, move.snd, false);
			});
		} catch (IOException | IllegalArgumentException e) {
			return false;
		}
		return true;
	}
}
