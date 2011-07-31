/*
Copyright 2008-2011 Gephi
Authors : Antonio Patriarca <antoniopatriarca@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.gephi.visualization.controller;


import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import org.gephi.graph.api.Node;
import org.gephi.lib.gleem.linalg.Vec3f;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.project.api.WorkspaceListener;
import org.gephi.visualization.api.controller.MotionManager;
import org.gephi.visualization.api.camera.Camera;
import org.gephi.visualization.api.controller.VisualizationController;
import org.gephi.visualization.api.selection.SelectionManager;
import org.gephi.visualization.api.vizmodel.VizModel;
import org.gephi.visualization.camera.Camera2d;
import org.gephi.visualization.camera.Camera3d;
import org.gephi.visualization.data.FrameDataBridge;
import org.gephi.visualization.geometry.AABB;
import org.gephi.visualization.model.Model;
import org.gephi.visualization.view.View;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implementation of visualization controller.
 *
 * @author Antonio Patriarca <antoniopatriarca@gmail.com>
 * @author Vojtech Bardiovsky <vojtech.bardiovsky@gmail.com>
 */
@ServiceProvider(service = VisualizationController.class)
public class VisualizationControllerImpl implements VisualizationController, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, WorkspaceListener {

    private Camera camera;

    private static VisualizationControllerImpl instance;
    private final View view;
    private final Model dataManager;
    private final FrameDataBridge frameDataBridge;

    private Dimension viewSize;

    private boolean centerGraph = true;
    private boolean centerZero;
    private float[] centerNode;

    private boolean use3d;

    private boolean hasWorkspace;

    // TODO remove when architecture bugs fixed
    private static final Camera DEFAULT_CAMERA = new Camera2d(300, 300, 100f, 10000.0f);

    public VisualizationControllerImpl() {
        // Random values
        this.viewSize = new Dimension();

        this.frameDataBridge = new FrameDataBridge();
        this.view = new View(this, this.frameDataBridge);
        this.dataManager = new Model(this, this.frameDataBridge, 33);

        Lookup.getDefault().lookup(ProjectController.class).addWorkspaceListener(this);
        
        // Initialize SelectionManager
        Lookup.getDefault().lookup(SelectionManager.class).initialize();
    }

    synchronized static VisualizationControllerImpl getDefault() {
        if (instance == null) {
            instance = (VisualizationControllerImpl) Lookup.getDefault().lookup(VisualizationController.class);
        }
        return instance;
    }

    public void resize(int width, int height) {
        this.viewSize = new Dimension(width, height);
        if (camera != null) {
            this.camera.setImageSize(viewSize);
        }
    }

    @Override
    public Dimension getViewDimensions() {
        return viewSize;
    }

    @Override
    public Point getViewLocationOnScreen() {
        return view.getCanvas().getLocationOnScreen();
    }

    @Override
    public Component getViewComponent() {
        return view.getCanvas();
    }

    public void setCursor(Cursor cursor) {
        view.getCanvas().setCursor(cursor);
    }

    @Override
    public Camera getCamera() {
        return this.camera;
    }

    @Override
    public Camera getCameraCopy() {
        // TODO remove when architecture bugs fixed
        if (camera == null) {
            return DEFAULT_CAMERA.copy();
        }
        return this.camera.copy();
    }

    @Override
    public void centerOnGraph() {
        centerGraph = true;
    }

    @Override
    public void centerOnZero() {
        centerZero = true;
    }

    @Override
    public void centerOnNode(Node node) {
        centerNode = new float[]{node.getNodeData().x(), node.getNodeData().y(), node.getNodeData().z()};
    }

    @Override
    public boolean isCentering() {
        return centerGraph || centerZero || centerNode != null;
    }

    @Override
    public void modeChanged() {
        boolean modelUse3d = Lookup.getDefault().lookup(VizModel.class).isUse3d();
        if (modelUse3d == this.use3d) {
            return;
        }
        Workspace workspace = Lookup.getDefault().lookup(ProjectController.class).getCurrentWorkspace();
        Camera cam = workspace.getLookup().lookup(Camera.class);
        workspace.remove(cam);
        if (modelUse3d) {
        // Set 2D mode
            if (cam instanceof Camera2d) {
                camera = new Camera3d((Camera2d) cam);
                
                // TODO add other engine code
                //
            }
        } else {
        // Set 3D mode
            if (cam instanceof Camera3d) {
                camera = new Camera2d((Camera3d) cam);

                // TODO add other engine code
                //
            }
        }
        workspace.add(camera);
        Lookup.getDefault().lookup(SelectionManager.class).refreshDataStructure();
    }

    public void beginUpdateFrame() {
    }

    public void endUpdateFrame(AABB box) {
        if (centerGraph && box != null) {
            final Vec3f center = box.center();
            final Vec3f scale = box.scale();
            final Vec3f minVec = box.minVec();
            final Vec3f maxVec = box.maxVec();

            float d = scale.y() / (float)Math.tan(0.5 * camera.fov());

            final Vec3f origin = new Vec3f(center.x(), center.y(), maxVec.z() + d*1.1f);
            camera.lookAt(origin, center, Vec3f.Y_AXIS);
            //camera.setClipPlanes(d, maxVec.z() - minVec.z() + d*1.2f);
            centerGraph = false;
        }
        if (centerZero) {
            camera.lookAt(Vec3f.Z_AXIS, new Vec3f(0, 0, 0), Vec3f.Y_AXIS);
            centerZero = false;
        }
        if (centerNode != null) {
            camera.lookAt(new Vec3f(centerNode[0], centerNode[1], centerNode[2]), Vec3f.Y_AXIS);
            centerNode = null;
        }
        Lookup.getDefault().lookup(MotionManager.class).refresh();
    }

    public void beginRenderFrame() {
    }

    public void endRenderFrame() {
    }

    @Override
    public void start() {
        this.dataManager.start();
        this.view.start();
    }

    @Override
    public void stop() {
        this.dataManager.stop();
        this.view.stop();
    }

    // User events
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (hasWorkspace) {
            Lookup.getDefault().lookup(MotionManager.class).mouseClicked(e);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (hasWorkspace) {
            Lookup.getDefault().lookup(MotionManager.class).mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (hasWorkspace) {
            Lookup.getDefault().lookup(MotionManager.class).mouseReleased(e);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (hasWorkspace) {
            Lookup.getDefault().lookup(MotionManager.class).mouseEntered(e);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (hasWorkspace) {
            Lookup.getDefault().lookup(MotionManager.class).mouseExited(e);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (hasWorkspace) {
            Lookup.getDefault().lookup(MotionManager.class).mouseDragged(e);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (hasWorkspace) {
            Lookup.getDefault().lookup(MotionManager.class).mouseMoved(e);
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (hasWorkspace) {
             Lookup.getDefault().lookup(MotionManager.class).mouseWheelMoved(e);
        }
    }

    // Workspace events
    @Override
    public void initialize(Workspace workspace) {
    }

    @Override
    public void select(Workspace workspace) {
        camera = workspace.getLookup().lookup(Camera.class);
        if (camera == null) {
            camera = new Camera2d(viewSize.width, viewSize.height, 100f, 10000.0f);
            workspace.add(camera);
        }
        hasWorkspace = true;
    }

    @Override
    public void unselect(Workspace workspace) {
    }

    @Override
    public void close(Workspace workspace) {
    }

    @Override
    public void disable() {
    }

}