/**
 * Created by kanari on 2016/7/24.
 */

package ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import logic.*;
import org.datafx.controller.FXMLController;
import util.TaskScheduler;

import java.util.function.BiConsumer;
import java.util.function.Function;

@FXMLController(value = "fxml/GameBoard.fxml", title = "Reversi Duel")
public class OnlineDuelGameBoardController extends AbstractGameBoardController {
	private BooleanProperty isMyself;

	@Override
	protected void initPlayersAndControls() {
		AbstractPlayer localPlayer, networkPlayer;
		if (!(player1 instanceof NetworkPlayer)) {
			localPlayer = player1;
			networkPlayer = player2;
		} else {
			localPlayer = player2;
			networkPlayer = player1;
		}

		if (localPlayer instanceof LocalPlayer) {
			((LocalPlayer) localPlayer).setInfoDialogCaller(this::showInfoDialog);
			((LocalPlayer) localPlayer).setConfirmDialogCaller(this::showConfirmDialog);
			((LocalPlayer) localPlayer).setWaitDialogCaller(this::showWaitDialog);
		}

		assert localPlayer instanceof LocalPlayer || localPlayer instanceof AIPlayer;
		assert networkPlayer instanceof NetworkPlayer;

		readyButton.setOnAction(e -> TaskScheduler.singleShot(1, () -> {
			if (localPlayer instanceof LocalPlayer) ((LocalPlayer) localPlayer).ready();
			else ((AIPlayer) localPlayer).ready();
		}));
		if (localPlayer instanceof LocalPlayer) {
			BiConsumer<Button, Function<LocalPlayer, Runnable>> setAction = (button, function) -> {
				button.setOnAction(e -> {
					LocalPlayer player = (LocalPlayer) localPlayer;
					TaskScheduler.singleShot(1, function.apply(player)); // make sure function is called on another thread!
				});
			};
			setAction.accept(undoButton, p -> p::requestUndo);
			setAction.accept(surrenderButton, p -> p::requestSurrender);
			setAction.accept(drawButton, p -> p::requestDraw);
		}
		sendChatButton.setOnAction(e -> TaskScheduler.singleShot(1, () -> {
			if (!chatText.getText().isEmpty()) {
				if (localPlayer instanceof LocalPlayer) ((LocalPlayer) localPlayer).sendChat(chatText.getText());
				else ((AIPlayer) localPlayer).sendChat(chatText.getText());
				chatText.setText("");
			}
		}));

		isMyself = new SimpleBooleanProperty(false);
		undoButton.setDisable(true);
		if (localPlayer instanceof LocalPlayer) {
			undoButton.disableProperty().bind(manager.gameStartedProperty().not().or(isMyself.not())
					.or(((LocalPlayer) localPlayer).canUndoProperty().not()));
		}
		surrenderButton.disableProperty().bind(manager.gameStartedProperty().not().or(isMyself.not()));
		drawButton.disableProperty().bind(manager.gameStartedProperty().not().or(isMyself.not()));
		saveButton.setDisable(true);
		loadButton.setDisable(true);
		manager.currentPlayerProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == PlayerState.NONE || !(manager.getPlayer(newValue) instanceof LocalPlayer)) {
				isMyself.setValue(false);
			} else {
				isMyself.setValue(true);
			}
		});
	}
}
