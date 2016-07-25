/**
 * Created by kanari on 2016/7/25.
 */

package CustomOverride;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.datafx.controller.context.ViewContext;
import org.datafx.controller.flow.FlowContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;


/**
 * Rewrite of original AnimatedFlowController in DataFX
 * Animation is set to SWIPE_LEFT for normal navigation, and SWIPE_RIGHT for navigating back
 *
 * Due to the fact that a number of functions are private but not protected in the original class,
 * I had to rewrite instead of extend the class
 */
public class CustomAnimatedFlowContainer implements FlowContainer<StackPane> {

	private Stack<Integer> historyViews;
	private StackPane root;
	private Duration duration;
	private Function<org.datafx.controller.flow.container.AnimatedFlowContainer, List<KeyFrame>> animationProducer;
	private Timeline animation;
	private ImageView placeholder;

	// Copied from ContainerAnimations, in order to match function signature
	private static Function<CustomAnimatedFlowContainer, List<KeyFrame>> SWIPE_LEFT = c ->
			new ArrayList<>(Arrays.asList(
					new KeyFrame(Duration.ZERO,
							new KeyValue(c.getView().translateXProperty(), c.getView().getWidth(), Interpolator.EASE_BOTH),
							new KeyValue(c.getPlaceholder().translateXProperty(), -c.getView().getWidth(), Interpolator.EASE_BOTH)),
					new KeyFrame(c.getDuration(),
							new KeyValue(c.getView().translateXProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(c.getPlaceholder().translateXProperty(), -c.getView().getWidth(), Interpolator.EASE_BOTH))
			));
	private static Function<CustomAnimatedFlowContainer, List<KeyFrame>> SWIPE_RIGHT = c ->
			new ArrayList<>(Arrays.asList(
					new KeyFrame(Duration.ZERO,
							new KeyValue(c.getView().translateXProperty(), -c.getView().getWidth(), Interpolator.EASE_BOTH),
							new KeyValue(c.getPlaceholder().translateXProperty(), c.getView().getWidth(), Interpolator.EASE_BOTH)),
					new KeyFrame(c.getDuration(),
							new KeyValue(c.getView().translateXProperty(), 0, Interpolator.EASE_BOTH),
							new KeyValue(c.getPlaceholder().translateXProperty(), c.getView().getWidth(), Interpolator.EASE_BOTH))
			));

	private BooleanProperty isInitialView;

	public boolean isIsInitialView() {
		return isInitialView.get();
	}

	public BooleanProperty isInitialViewProperty() {
		return isInitialView;
	}

	/**
	 * Creates a container with the given animation type and duration
	 *
	 * @param duration the duration of the animation
	 */
	public CustomAnimatedFlowContainer(Duration duration) {
		this.root = new StackPane();
		this.duration = duration;
		placeholder = new ImageView();
		placeholder.setPreserveRatio(true);
		placeholder.setSmooth(true);
		historyViews = new Stack<>();
		isInitialView = new SimpleBooleanProperty(true);
	}

	@Override
	public <U> void setViewContext(ViewContext<U> context) {
		updatePlaceholder(context.getRootNode());

		if (animation != null) {
			animation.stop();
		}
		animation = new Timeline();

		for (Integer hash : historyViews) {
			System.out.print(hash + " ");
		}
		System.out.println();

		Integer hash = context.getController().getClass().hashCode();
		if (historyViews.size() > 1 && hash.equals(historyViews.get(historyViews.size() - 2))) {
			historyViews.pop();
			animation.getKeyFrames().addAll(SWIPE_RIGHT.apply(this));
		} else {
			animation.getKeyFrames().addAll(SWIPE_LEFT.apply(this));
			historyViews.push(hash);
		}

		animation.getKeyFrames().add(new KeyFrame(duration, (e) -> clearPlaceholder()));
		animation.play();
		System.out.println("View stack size: " + historyViews.size());
		isInitialView.setValue(historyViews.size() == 1);
	}

	/**
	 * Returns the {@link ImageView} instance that is used as a placeholder for the old view in each navigation animation.
	 *
	 * @return
	 */
	public ImageView getPlaceholder() {
		return placeholder;
	}

	/**
	 * Returns the duration for the animation
	 *
	 * @return the duration for the animation
	 */
	public Duration getDuration() {
		return duration;
	}

	@Override
	public StackPane getView() {
		return root;
	}

	private void clearPlaceholder() {
		placeholder.setImage(null);
		placeholder.setVisible(false);
	}

	private void updatePlaceholder(Node newView) {
		if (root.getWidth() > 0 && root.getHeight() > 0) {
			SnapshotParameters parameters = new SnapshotParameters();
			parameters.setFill(Color.TRANSPARENT);
			Image placeholderImage = root.snapshot(parameters, new WritableImage((int) root.getWidth(), (int) root.getHeight()));
			placeholder.setImage(placeholderImage);
			placeholder.setFitWidth(placeholderImage.getWidth());
			placeholder.setFitHeight(placeholderImage.getHeight());
		} else {
			placeholder.setImage(null);
		}
		placeholder.setVisible(true);
		placeholder.setOpacity(1.0);
		root.getChildren().setAll(placeholder);
		root.getChildren().add(newView);
		placeholder.toFront();
	}
}
