/**
 * Created by kanari on 2016/7/29.
 */

package logic;

import java.awt.Point;
import java.util.List;

public class GameManagerInterface {
	private GameManager manager;
	private PlayerState player;

	public GameManagerInterface(GameManager manager, PlayerState player) {
		this.manager = manager;
		this.player = player;
	}

	// call at your own risk
	public void forceExit(String message) {
		manager.forceExit(message);
	}

	public void sendChat(String message) {
		manager.sendChat(player, message);
	}

	public boolean isReady() {
		return manager.isReady(player);
	}

	public void ready() {
		manager.ready(player);
	}

	public boolean canUndo() {
		return manager.canUndo(player);
	}

	public boolean requestUndo() {
		return manager.requestUndo(player);
	}

	public boolean requestDraw() {
		return manager.requestDraw(player);
	}

	public boolean requestSurrender() {
		return manager.requestSurrender(player);
	}

	public boolean requestExit() {
		return manager.requestExit(player);
	}

	public boolean canDrop(int x, int y) {
		return manager.canDrop(x, y);
	}

	public boolean isMyTurn() {
		return manager.getCurrentPlayer() == player;
	}

	public boolean dropPiece(int x, int y) {
		if (!isMyTurn() || !canDrop(x, y)) return false;
		manager.dropPiece(x, y, player, false);
		return true;
	}

	public PlayerState getState(int x, int y) {
		return manager.getState(x, y);
	}

	public List<Point> getCandidatePositions() {
		return manager.getCandidatePositions();
	}

	public List<Point> getFlippedPositions(int x, int y) {
		return manager.getFlippedPositions(x, y, player);
	}
}
