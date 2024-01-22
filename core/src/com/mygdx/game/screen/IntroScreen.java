package com.mygdx.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.VirtualRunner;
import com.mygdx.game.assets.AssetDescriptors;
import com.mygdx.game.assets.RegionNames;
import com.mygdx.game.config.GameConfig;


public class IntroScreen extends ScreenAdapter {

    public static final float INTRO_DURATION_IN_SEC = 2.5f;   // duration of the (intro) animation

    private final VirtualRunner game;
    private final AssetManager assetManager;

    private Viewport viewport;
    private TextureAtlas gameplayAtlas;

    private float duration = 0f;

    private Stage stage;
    private String[] frames = {RegionNames.FRAME_1, RegionNames.FRAME_2, RegionNames.FRAME_3, RegionNames.FRAME_4, RegionNames.FRAME_5, RegionNames.FRAME_6, RegionNames.FRAME_7, RegionNames.FRAME_8};
    private int currentNumberIndex = 0;
    private Image background;
    private Image frame;
    private Skin skin;

    public IntroScreen(VirtualRunner game) {
        this.game = game;
        assetManager = game.getAssetManager();
        viewport = new FitViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());
        assetManager.load(AssetDescriptors.UI_FONT);
        assetManager.load(AssetDescriptors.UI_SKIN);
        assetManager.load(AssetDescriptors.GAMEPLAY);
        assetManager.finishLoading();
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);
        skin = assetManager.get(AssetDescriptors.UI_SKIN);
    }

    @Override
    public void show() {

        frame = new Image(gameplayAtlas.findRegion(RegionNames.FRAME_1)); // Initialize dice here
        frame.setOrigin(Align.bottomLeft);
        frame.setPosition(50f, 0f);
        rollFilm();

        stage.addActor(frame);
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(255, 255, 255, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        duration += delta;

//         go to the MenuScreen after INTRO_DURATION_IN_SEC seconds
        if (duration > INTRO_DURATION_IN_SEC) {
            game.setScreen(new MapScreen(game));
        }

        stage.act(delta);
        stage.draw();
        if (frame.getActions().size == 0) {
            rollFilm();
        }
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    private void rollFilm() {
        frame.clearActions();
        frame.setScale(0.3f);

        Label label = new Label("Virtual Runner", skin, "alt");
        label.setColor(1, 1, 1, 1);

        stage.addActor(label);

        label.setPosition(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2 + label.getHeight());

        frame.addAction(Actions.sequence(
                Actions.run(new Runnable() {

                    @Override
                    public void run() {
                        currentNumberIndex = (currentNumberIndex + 1) % 8;
                        frame.setDrawable(new TextureRegionDrawable(gameplayAtlas.findRegion(frames[currentNumberIndex])));

                    }
                }),
                Actions.delay(0.05f), // Add a delay here

                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        rollFilm();
                    }
                })
        ));
    }

}

