package com.getting.gifconvert;

import binding.MediaDurationLabelFormatter;
import binding.MediaDurationStringFormatter;
import binding.NullableObjectStringFormatter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import media.GifConvertParameters;
import media.GifConvertResult;
import media.GifConverter;
import media.VideoInfo;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.StatusBar;
import org.controlsfx.control.ToggleSwitch;
import com.getting.util.Looper;
import com.getting.util.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private static final Object MSG_HIDE_NOTIFICATION = new Object();

    private static final Object MSG_CONVERT_MEDIA = new Object();

    private static final Object MSG_RELOAD_MEDIA_INFO = new Object();

    private final GifConverter gifConverter = new GifConverter();

    private final Image loadingImage = new Image(MainController.class.getResource("loading.gif").toExternalForm(), true);

    @FXML
    private ImageView outputPreviewView;

    @FXML
    private Slider outputFrameRateView;

    @FXML
    private Slider outputScaleView;

    @FXML
    private RangeSlider inputMediaDurationView;

    @FXML
    private Label inputMediaStartTimeView;

    @FXML
    private Label inputMediaEndTimeView;

    @FXML
    private Pane inputMediaDurationPane;

    @FXML
    private CheckMenuItem reverseOutputView;

    @FXML
    private CheckMenuItem addLogoView;

    @FXML
    private ToggleSwitch detailView;

    @FXML
    private Label mediaInfoView;

    @FXML
    private NotificationPane notificationPane;

    @FXML
    private StatusBar statusBar;

    private ObjectProperty<File> inputMedia = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoadingImage();

        statusBar.progressProperty().bind(gifConverter.convertProgressProperty());
        mediaInfoView.textProperty().bind(new NullableObjectStringFormatter<>(gifConverter.videoInfoProperty()));
        inputMediaStartTimeView.textProperty().bind(new MediaDurationStringFormatter(inputMediaDurationView.lowValueProperty()));
        inputMediaEndTimeView.textProperty().bind(new MediaDurationStringFormatter(inputMediaDurationView.highValueProperty()));
        inputMediaDurationView.setLabelFormatter(new MediaDurationLabelFormatter());

        {
            final ChangeListener<Number> convertParameterChangeListener = new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    reloadConvertDuration();
                    reloadMediaConvert(1000);
                }

            };

            inputMediaDurationView.lowValueProperty().addListener(convertParameterChangeListener);
            inputMediaDurationView.highValueProperty().addListener(convertParameterChangeListener);
        }

        {
            final ChangeListener<Number> convertParameterChangeListener = new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    reloadMediaConvert(1000);
                }

            };

            outputScaleView.valueProperty().addListener(convertParameterChangeListener);
            outputFrameRateView.valueProperty().addListener(convertParameterChangeListener);
        }

        {
            final ChangeListener<Boolean> convertParameterChangeListener = new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    reloadMediaConvert(0);
                }

            };

            reverseOutputView.selectedProperty().addListener(convertParameterChangeListener);
            addLogoView.selectedProperty().addListener(convertParameterChangeListener);
        }

        inputMedia.addListener(new ChangeListener<File>() {

            @Override
            public void changed(ObservableValue<? extends File> observable, File oldValue, File newValue) {
                reloadMediaInfo();
            }

        });

        gifConverter.videoInfoProperty().addListener(new ChangeListener<VideoInfo>() {

            @Override
            public void changed(ObservableValue<? extends VideoInfo> observable, VideoInfo oldValue, VideoInfo newValue) {
                reloadConvertDuration();
            }

        });

        detailView.selectedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                reloadConvertDuration();
            }

        });

        outputPreviewView.setOnDragOver(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.LINK);
            }

        });
        outputPreviewView.setOnDragDropped(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                List<File> files = event.getDragboard().getFiles();
                if (!files.isEmpty()) {
                    inputMedia.set(files.get(0));
                }
            }

        });
    }

    @FXML
    private void onChooseVideo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("视频文件", GifConvertParameters.SUPPORT_VIDEO_FORMATS));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("所有文件", "*.*"));

        File chooseFile = fileChooser.showOpenDialog(outputPreviewView.getScene().getWindow());
        if (chooseFile != null) {
            inputMedia.set(chooseFile);
        }
    }

    @FXML
    private void onOpenSaveDirectory(ActionEvent event) {
        if (inputMedia.get() == null) {
            return;
        }

        if (!inputMedia.get().exists()) {
            return;
        }

        try {
            java.awt.Desktop.getDesktop().open(inputMedia.get().getParentFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reloadConvertDuration() {
        if (gifConverter.videoInfoProperty().get() == null) {
            return;
        }

        final double mediaDuration = gifConverter.videoInfoProperty().get().getDuration();

        if (detailView.isSelected()) {
            inputMediaDurationView.setMin(Math.max(0, inputMediaDurationView.getLowValue() - 10));
            inputMediaDurationView.setMax(Math.min(mediaDuration, inputMediaDurationView.getHighValue() + 10));
        } else {
            inputMediaDurationView.setMin(0);
            inputMediaDurationView.setMax(mediaDuration);
        }
        inputMediaDurationView.setMajorTickUnit((inputMediaDurationView.getMax() - inputMediaDurationView.getMin()) / 10);

        inputMediaDurationPane.setVisible(true);
    }

    private void reloadMediaConvert(long delay) {
        Looper.removeTask(MSG_CONVERT_MEDIA);

        notificationPane.hide();

        if (inputMedia.get() == null) {
            return;
        }

        if (!inputMedia.get().exists() || !inputMedia.get().isFile()) {
            notificationPane.show("所选择的文件已被删除，请重新选择文件");
            return;
        }

        if (inputMediaDurationView.getHighValue() - inputMediaDurationView.getLowValue() > 30) {
            notificationPane.show("转换时间长度过长");
            return;
        }

        if (inputMediaDurationView.getHighValue() - inputMediaDurationView.getLowValue() < 1) {
            return;
        }

        Looper.postTask(new ConvertMediaTask(delay));
    }

    private void reloadMediaInfo() {
        Looper.removeTask(MSG_RELOAD_MEDIA_INFO);
        if (inputMedia.get() == null) {
            return;
        }
        Looper.postTask(new ReloadMediaInfoTask());
    }

    private void showLoadingImage() {
        outputPreviewView.setImage(loadingImage);
    }

    private void showLoadingFinish(GifConvertResult result) {
        if (result == null) {
            return;
        }

        try {
            outputPreviewView.setImage(new Image(result.getOutputFile().toURI().toURL().toExternalForm(), true));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (result.isCanceled()) {
            return;
        }

        showNotificationForAWhile(result.getResult());
    }

    private void showNotificationForAWhile(String message) {
        notificationPane.show(message);

        Looper.removeTask(MSG_HIDE_NOTIFICATION);
        Looper.postTask(new HideNotificationTask(3000));
    }

    private class HideNotificationTask extends AsyncTask<Void> {

        public HideNotificationTask(long delay) {
            super(MSG_HIDE_NOTIFICATION, delay);
        }

        @Override
        public void preTaskOnUi() {
            notificationPane.hide();
        }

        @Override
        public Void runTask() {
            return null;
        }

        @Override
        public void postTaskOnUi(Void result) {
        }

        @Override
        public void cancel() {
        }

    }

    private class ReloadMediaInfoTask extends AsyncTask<Void> {

        public ReloadMediaInfoTask() {
            super(MSG_RELOAD_MEDIA_INFO, 0);
        }

        @Override
        public void preTaskOnUi() {

        }

        @Override
        public Void runTask() {
            gifConverter.updateVideo(inputMedia.get());
            return null;
        }

        @Override
        public void postTaskOnUi(Void result) {

        }

        @Override
        public void cancel() {
            gifConverter.cancel();
        }

    }

    private class ConvertMediaTask extends AsyncTask<GifConvertResult> {

        public ConvertMediaTask(long delay) {
            super(MSG_CONVERT_MEDIA, delay);
        }

        @Override
        public void preTaskOnUi() {
            showLoadingImage();
        }

        @Override
        public GifConvertResult runTask() {
            String logo = addLogoView.isSelected() ? new SimpleDateFormat().format(new Date()) : " ";
            return gifConverter.convert(
                    new GifConvertParameters(inputMedia.get(),
                            outputFrameRateView.getValue(),
                            outputScaleView.getValue(),
                            inputMediaDurationView.getLowValue(),
                            inputMediaDurationView.getHighValue() - inputMediaDurationView.getLowValue(),
                            reverseOutputView.isSelected(),
                            logo));
        }

        @Override
        public void postTaskOnUi(GifConvertResult result) {
            showLoadingFinish(result);
        }

        @Override
        public void cancel() {
            gifConverter.cancel();
        }

    }

}
