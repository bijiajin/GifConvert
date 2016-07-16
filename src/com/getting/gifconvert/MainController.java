package com.getting.gifconvert;

import binding.MediaDurationLabelFormatter;
import binding.MediaDurationStringFormatter;
import com.getting.util.binding.NullableObjectStringFormatter;
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
    private ImageView gifPreviewView;

    @FXML
    private Slider gifFrameRateView;

    @FXML
    private Slider gifScaleView;

    @FXML
    private RangeSlider inputVideoDurationView;

    @FXML
    private Label inputVideoStartTimeView;

    @FXML
    private Label inputVideoEndTimeView;

    @FXML
    private Pane inputVideoDurationPane;

    @FXML
    private CheckMenuItem reverseGifView;

    @FXML
    private CheckMenuItem addLogoView;

    @FXML
    private ToggleSwitch videoDurationDetailView;

    @FXML
    private Label videoInfoView;

    @FXML
    private NotificationPane notificationPane;

    @FXML
    private StatusBar statusBar;

    private ObjectProperty<File> inputVideo = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoadingImage();

        statusBar.progressProperty().bind(gifConverter.convertProgressProperty());
        videoInfoView.textProperty().bind(new NullableObjectStringFormatter<>(gifConverter.videoInfoProperty()));
        inputVideoStartTimeView.textProperty().bind(new MediaDurationStringFormatter(inputVideoDurationView.lowValueProperty()));
        inputVideoEndTimeView.textProperty().bind(new MediaDurationStringFormatter(inputVideoDurationView.highValueProperty()));
        inputVideoDurationView.setLabelFormatter(new MediaDurationLabelFormatter());

        {
            final ChangeListener<Number> convertParameterChangeListener = new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    reloadInputDuration();
                    reloadGifConvert(1000);
                }

            };

            inputVideoDurationView.lowValueProperty().addListener(convertParameterChangeListener);
            inputVideoDurationView.highValueProperty().addListener(convertParameterChangeListener);
        }

        {
            final ChangeListener<Number> convertParameterChangeListener = new ChangeListener<Number>() {

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    reloadGifConvert(1000);
                }

            };

            gifScaleView.valueProperty().addListener(convertParameterChangeListener);
            gifFrameRateView.valueProperty().addListener(convertParameterChangeListener);
        }

        {
            final ChangeListener<Boolean> convertParameterChangeListener = new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    reloadGifConvert(0);
                }

            };

            reverseGifView.selectedProperty().addListener(convertParameterChangeListener);
            addLogoView.selectedProperty().addListener(convertParameterChangeListener);
        }

        inputVideo.addListener(new ChangeListener<File>() {

            @Override
            public void changed(ObservableValue<? extends File> observable, File oldValue, File newValue) {
                reloadVideoInfo();
            }

        });

        gifConverter.videoInfoProperty().addListener(new ChangeListener<VideoInfo>() {

            @Override
            public void changed(ObservableValue<? extends VideoInfo> observable, VideoInfo oldValue, VideoInfo newValue) {
                reloadInputDuration();
            }

        });

        videoDurationDetailView.selectedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                reloadInputDuration();
            }

        });

        gifPreviewView.setOnDragOver(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.LINK);
            }

        });
        gifPreviewView.setOnDragDropped(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                List<File> files = event.getDragboard().getFiles();
                if (!files.isEmpty()) {
                    inputVideo.set(files.get(0));
                }
            }

        });
    }

    @FXML
    private void onChooseVideo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("视频文件", GifConvertParameters.SUPPORT_VIDEO_FORMATS));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("所有文件", "*.*"));

        File chooseFile = fileChooser.showOpenDialog(gifPreviewView.getScene().getWindow());
        if (chooseFile != null) {
            inputVideo.set(chooseFile);
        }
    }

    @FXML
    private void onOpenSaveDirectory(ActionEvent event) {
        if (inputVideo.get() == null) {
            return;
        }

        if (!inputVideo.get().exists()) {
            return;
        }

        try {
            java.awt.Desktop.getDesktop().open(inputVideo.get().getParentFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reloadInputDuration() {
        if (gifConverter.videoInfoProperty().get() == null) {
            return;
        }

        final double duration = gifConverter.videoInfoProperty().get().getDuration();

        if (videoDurationDetailView.isSelected()) {
            inputVideoDurationView.setMin(Math.max(0, inputVideoDurationView.getLowValue() - 10));
            inputVideoDurationView.setMax(Math.min(duration, inputVideoDurationView.getHighValue() + 10));
        } else {
            inputVideoDurationView.setMin(0);
            inputVideoDurationView.setMax(duration);
        }
        inputVideoDurationView.setMajorTickUnit((inputVideoDurationView.getMax() - inputVideoDurationView.getMin()) / 10);

        inputVideoDurationPane.setVisible(true);
    }

    private void reloadGifConvert(long delay) {
        Looper.removeTask(MSG_CONVERT_MEDIA);

        notificationPane.hide();

        if (inputVideo.get() == null) {
            return;
        }

        if (!inputVideo.get().exists() || !inputVideo.get().isFile()) {
            notificationPane.show("所选择的文件已被删除，请重新选择文件");
            return;
        }

        if (inputVideoDurationView.getHighValue() - inputVideoDurationView.getLowValue() > 30) {
            notificationPane.show("转换时间长度过长");
            return;
        }

        if (inputVideoDurationView.getHighValue() - inputVideoDurationView.getLowValue() < 1) {
            return;
        }

        Looper.postTask(new GifConvertTask(delay));
    }

    private void reloadVideoInfo() {
        Looper.removeTask(MSG_RELOAD_MEDIA_INFO);
        if (inputVideo.get() == null) {
            return;
        }
        Looper.postTask(new ReloadVideoInfoTask());
    }

    private void showLoadingImage() {
        gifPreviewView.setImage(loadingImage);
    }

    private void showLoadingFinish(GifConvertResult result) {
        if (result == null) {
            return;
        }

        try {
            gifPreviewView.setImage(new Image(result.getOutputFile().toURI().toURL().toExternalForm(), true));
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

    }

    private class ReloadVideoInfoTask extends AsyncTask<Void> {

        public ReloadVideoInfoTask() {
            super(MSG_RELOAD_MEDIA_INFO, 0);
        }

        @Override
        public Void runTask() {
            gifConverter.updateVideo(inputVideo.get());
            return null;
        }

        @Override
        public void cancel() {
            gifConverter.cancel();
        }

    }

    private class GifConvertTask extends AsyncTask<GifConvertResult> {

        public GifConvertTask(long delay) {
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
                    new GifConvertParameters(inputVideo.get(),
                            gifFrameRateView.getValue(),
                            gifScaleView.getValue(),
                            inputVideoDurationView.getLowValue(),
                            inputVideoDurationView.getHighValue() - inputVideoDurationView.getLowValue(),
                            reverseGifView.isSelected(),
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