package com.mygdx.game;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mygdx.game.assets.AssetDescriptors;
import com.mygdx.game.assets.RegionNames;
import com.mygdx.game.utils.Geolocation;
import com.mygdx.game.utils.MapRasterTiles;
import com.mygdx.game.utils.ZoomXY;

public class RunnerAnimation {
    private Geolocation[] geolocations;
    private Vector2[] positions;
    private Vector2[] interpolatedPositions;
    private TextureAtlas gameplayAtlas;
    private final VirtualRunner game;
    private final AssetManager assetManager;

    public RunnerAnimation(Geolocation[] geolocations, ZoomXY beginTile, int numInterpolatedPoints, VirtualRunner game) {
        this.geolocations = geolocations;
        positions = positionsFromGeolocations(geolocations, beginTile);
        interpolatedPositions = getInterpolatedPositions(positions, numInterpolatedPoints);
        this.game = game;
        assetManager = game.getAssetManager();
        assetManager.load(AssetDescriptors.PARTICLE_SNOW);
        assetManager.load(AssetDescriptors.PARTICLE_RAIN);
        assetManager.load(AssetDescriptors.UI_SKIN);
        assetManager.load(AssetDescriptors.GAMEPLAY);
        assetManager.load(AssetDescriptors.UI_FONT);
        assetManager.finishLoading();
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);
    }

    public Actor create() {
        Image man = new Image(new TextureRegionDrawable(gameplayAtlas.findRegion(RegionNames.MAN)));
        man.setWidth(40f);
        man.setHeight(50f);

        man.setPosition(interpolatedPositions[0].x, interpolatedPositions[0].y);
        man.setRotation(10);
        SequenceAction sequenceAction = new SequenceAction();
        float duration = 0.1f;
        for(int i=1; i<interpolatedPositions.length; i++){
            sequenceAction.addAction(
                    Actions.sequence(
                            Actions.moveTo(interpolatedPositions[i].x, interpolatedPositions[i].y, duration),
                            Actions.moveBy(0, 5, duration), // Move up
                            Actions.moveBy(0, -5, duration) // Move down
                    )
            );
        }
        sequenceAction.addAction(Actions.removeActor());
        man.addAction(sequenceAction);

        return man;
    }

    public Geolocation[] getGeolocations() {
        return geolocations;
    }

    public void setGeolocations(Geolocation[] geolocations, ZoomXY beginTile, int numInterpolatedPoints) {
        this.geolocations = geolocations;
        this.positions = positionsFromGeolocations(geolocations, beginTile);
        this.interpolatedPositions = getInterpolatedPositions(positions, numInterpolatedPoints);
    }

    public Vector2[] getPositions() {
        return positions;
    }

    public Vector2[] getInterpolatedPositions() {
        return interpolatedPositions;
    }

    static private Vector2[] positionsFromGeolocations(Geolocation[] geolocations, ZoomXY beginTile) {
        Vector2[] positions = new Vector2[geolocations.length];
        for (int i = 0; i < geolocations.length; i++)
            positions[i] = MapRasterTiles.getPixelPosition(geolocations[i].lat, geolocations[i].lng, beginTile.x, beginTile.y);
        return positions;
    }

    static private Vector2[] getInterpolatedPositions(Vector2[] positions, int num) {
        Vector2[] interpolatedAll = new Vector2[positions.length + (positions.length - 1) * num];

        for (int i = 0; i < positions.length - 1; i++) {
            interpolatedAll[i * (num + 1)] = positions[i];
            Vector2[] interpolatedPos = getInterpolatedPositions(positions[i], positions[i + 1], num, 5);
            for (int j = 0; j < interpolatedPos.length; j++) {
                interpolatedAll[i * (num + 1) + j + 1] = interpolatedPos[j];
            }
        }
        interpolatedAll[interpolatedAll.length - 1] = positions[positions.length - 1];
        return interpolatedAll;
    }

    static private Vector2[] getInterpolatedPositions(Vector2 point1, Vector2 point2, int num, float deviation) {
        Vector2[] positions = new Vector2[num];

        // linear equation
        float m = (point2.y - point1.y) / (point2.x - point1.x);
        float b = point1.y - m * point1.x;

        float distanceX = point2.x - point1.x;
        float deltaX = distanceX / (num + 1);
        float x, y;
        for (int i = 0; i < num; i++) {
            x = point1.x + deltaX + deltaX * i;
            y = m * x + b;
            if (i % 2 == 0) {
                y += deviation;
                x += deviation;
            }
            positions[i] = new Vector2(x, y);
        }

        return positions;
    }
}
