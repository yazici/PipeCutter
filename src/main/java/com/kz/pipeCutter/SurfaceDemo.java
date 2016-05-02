package com.kz.pipeCutter;

import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.camera.AbstractCameraController;
import org.jzy3d.chart.controllers.mouse.picking.AWTMousePickingController;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.picking.IObjectPickedListener;
import org.jzy3d.picking.PickingSupport;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Sphere;
import org.jzy3d.plot3d.rendering.canvas.CanvasAWT;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import org.jzy3d.plot3d.text.align.Halign;
import org.jzy3d.plot3d.text.align.Valign;
import org.jzy3d.plot3d.text.drawable.DrawableTextBitmap;

import com.kz.pipeCutter.BBB.Discoverer;
import com.kz.pipeCutter.ui.Settings;

public class SurfaceDemo extends AbstractAnalysis {
	Utils utils;
	Discoverer discoverer;
	MyTelnetClient smoothie;
	// private Cylinder cylinder = null;
	Sphere plasma = null;
	MyPickablePoint lastClickedPoint;
	String angleTxt = "0";
	public static SurfaceDemo instance;
	public MyComposite myComposite;
	public static boolean NUMBER_EDGES = false;
	public static boolean NUMBER_POINTS = false;
	public static boolean ZOOM_POINT = false;
	protected static float zoomBounds = 5;
	CanvasAWT canvas = null;
	private PickingSupport pickingSupport = null;

	private Coord3d rotationPoint;
	private DrawableTextBitmap currentRotTxt = null;
	public Point cylinderPoint;
	public Settings settingsFrame;
	
	public float plasmaKerfOffset = 0.5f;

	public SurfaceDemo()
	{
		System.setProperty("java.net.preferIPv4Stack", "true");
		//discoverer = new Discoverer();
		settingsFrame = new Settings();
		settingsFrame.setVisible(true);
	}
	
	public static void main(String[] args) throws Exception {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		instance = new SurfaceDemo();
		AnalysisLauncher.open(instance);
		System.out.println("Is daemon: " + Thread.currentThread().isDaemon());
		System.out.println("Is eventdispatched thread? " + javax.swing.SwingUtilities.isEventDispatchThread());

		instance.canvas = (CanvasAWT) instance.getChart().getCanvas();
		instance.canvas.getAnimator().start();
		// instance.canvas.getAnimator().setUpdateFPSFrames(20, System.out);
		instance.canvas.setSize(600, 600);

		MyPopupMenu menu = new MyPopupMenu();
		instance.canvas.add(menu);
		instance.canvas.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				if (arg0.getButton() == MouseEvent.BUTTON3) {

					SwingWorker menuShower = new SwingWorker<String, Object>() {
						@Override
						protected String doInBackground() throws Exception {
							// TODO Auto-generated method stub
							menu.show(instance.canvas, arg0.getX(), arg0.getY());
							return "Menu closed.";
						}
					};
					menuShower.execute();

				}
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
		// remove horizontal edges that connect to separated surfaces
		// edges with two point index 0 and 1

//		// instance.centerObject();
//		menu.addSeparator();
//		MenuItem menuItem11 = new MenuItem("Start animator");
//		menuItem11.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				SurfaceDemo.instance.canvas.getAnimator().start();
//				// SurfaceDemo.instance.resumeAnimator();
//				// SurfaceDemo.instance.canvas.getAnimator().setUpdateFPSFrames(20,
//				// System.out);
//			}
//		});
//		menu.add(menuItem11);

		instance.getChart().getView().setViewPositionMode(ViewPositionMode.FREE);
		instance.getChart().getView().setMaximized(true);
		// Iterator<AbstractCameraController> itController =
		// instance.getChart().getControllers().iterator();
		// while (itController.hasNext()) {
		// AbstractCameraController controller = itController.next();
		// if (controller instanceof ICameraKeyController) {
		// controller.dispose();
		// itController.remove();
		// }
		// }

	}

	private void enablePickingTexts(ArrayList<PickableDrawableTextBitmap> edgeTexts, Chart chart, int i) {
		if (getPickingSupport() != null) {
			for (PickableDrawableTextBitmap t : edgeTexts) {
				getPickingSupport().registerPickableObject(t, t);
			}

			getPickingSupport().addObjectPickedListener(new IObjectPickedListener() {
				@Override
				public void objectPicked(List<?> picked, PickingSupport ps) {
					if (picked.size() > 0) // && (System.currentTimeMillis() -
					// camMouse.clickTimeMillis < 200)) {
					{
						// System.out.println("Size: " + picked.size());
						// for (int i = 0; i < picked.size(); i++) {
						if (picked.get(0) instanceof PickableDrawableTextBitmap) {
							final PickableDrawableTextBitmap e = ((PickableDrawableTextBitmap) picked.get(0));
							final Integer edgeNo = Integer.valueOf(e.getText());
							final JPopupMenu menu = new JPopupMenu();
							JMenuItem menuItem = new JMenuItem("Remove edge: " + edgeNo);
							menuItem.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent arg0) {
									MyEdge edge = utils.edges.get(edgeNo);
									myComposite.remove(edge.lineStrip);
									myComposite.remove(e);
									instance.getChart().render();
									utils.edges.remove(edgeNo);
									// getPickingSupport().unRegisterAllPickableObjects();
									// initDraw();
								}
							});
							menu.add(menuItem);
							java.awt.Point point = MouseInfo.getPointerInfo().getLocation();
							SwingUtilities.convertPointFromScreen(point, canvas);
							menu.show(instance.canvas, point.x, point.y);
						}
					}
				}
			});
		}
	}

	private PickingSupport getPickingSupport() {
		if (instance.pickingSupport == null) {
			// TODO: Need to ask Martin about this next line of code not working in
			// Linux, but working
			// in Windows
			if (OSValidator.isWindows()) {
				AWTMousePickingController mousePicker = new AWTMousePickingController();
				instance.getChart().addController(mousePicker);
				instance.pickingSupport = mousePicker.getPickingSupport();
			}
			// AWTMousePickingController mousePicker = null;
			// for (AbstractCameraController controller : chart.getControllers()) {
			// if (controller instanceof AWTMousePickingController) {
			// mousePicker = (AWTMousePickingController) controller;
			// instance.pickingSupport = mousePicker.getPickingSupport();
			// }
			// }
		}
		return instance.pickingSupport;
	}

	@Override
	public void init() {
		try {

			System.out.println("Creating chart! Thread: " + Thread.currentThread().getName());
			smoothie = new MyTelnetClient(SmoothieUploader.smoothieIP, SmoothieUploader.smoothieRemotePort);
			// Create a chart
			chart = AWTChartComponentFactory.chart(Quality.Advanced, getCanvasType());
			canvas = (CanvasAWT) chart.getCanvas();

			// chart = newt SwingChartComponentFactory.chart(Quality.Advanced);
			// chart.getView().setMaximized(true);
			// chart.getView().setBoundManual(instance.chart.getView().getBounds());

			// canvas.getAnimator().start();
			// System.out.println("Animator started?: " + chart.ge
			// .getAnimator().isStarted());

			String filePath = null;
			if (OSValidator.isUnix())
				filePath = "./";
			else
				filePath = "./";

			Path path = FileSystems.getDefault().getPath(filePath, "data.csv");
			List<String> lines;
			lines = Files.readAllLines(path, Charset.forName("UTF-8"));
			utils = new Utils();

			ArrayList<String> line = new ArrayList<String>();

			// Point previousPolyPoint = null;
			LineStrip ls_p = null;
			int previousSurfaceNo = -1;
			int previousEdgeNo = -1;
			int edgeNoInSurface = -1;
			int pointId = -1;

			int edgeNo = 0;
			for (String string : lines) {
				line.add(string);
				String splitted[] = string.split(";");
				int surfaceNo = Integer.valueOf(splitted[0].split("_")[1]);
				int inventorEdgeNo = Integer.valueOf(splitted[0].split("_")[2]);
				int colorNo = inventorEdgeNo % Color.COLORS.length;

				if (surfaceNo != previousSurfaceNo)
					edgeNoInSurface = 0;

				if (splitted[0].startsWith("POLY")) {
					MyPickablePoint mp2 = null;
					MyPickablePoint mp1 = null;
					for (int i = 1; i < splitted.length; i = i + 3) {
						Float x = Float.valueOf(splitted[i].replace(",", "."));
						Float y = Float.valueOf(splitted[i + 1].replace(",", "."));
						Float z = Float.valueOf(splitted[i + 2].replace(",", "."));
						Coord3d p = new Coord3d(x.floatValue(), y.floatValue(), z.floatValue());
						pointId++;
						mp2 = utils.createOrFindMyPickablePoint(pointId, p, inventorEdgeNo);
						if (mp1 != null && mp2.distance(mp1) > 0) {
							// try to find edge with this two points
							boolean found = false;
							for (MyEdge e : utils.edges.values()) {
								if ((e.getPointByIndex(0).distance(mp1) == 0 && (e.getPointByIndex(1).distance(mp2) == 0) || (e
										.getPointByIndex(1).distance(mp1) == 0 && (e.getPointByIndex(0).distance(mp2) == 0)))) {
									found = true;
									break;
								}
							}
							if (!found) {
								MyEdge edge = new MyEdge(edgeNo, surfaceNo);
								edge.addPoint(mp1.id);
								edge.addPoint(mp2.id);
								MySurface surface = utils.surfaces.get(new Integer(surfaceNo));
								if (surface == null) {
									surface = new MySurface(surfaceNo);
									utils.surfaces.put(new Integer(surfaceNo), surface);
								}
								surface.addEdge(edge);
								utils.edges.put(edgeNo, edge);
								edgeNo++;
							}
						}
						mp1 = mp2;
					}
				}

			}

			
			splitLongEdges(pointId, edgeNo);
			utils.establishNeighbourPoints();
			utils.calculateContinuousEdges();

			System.out.println("Points: " + utils.points.size());
			System.out.println("Edges: " + utils.edges.size());
			System.out.println("Surfaces: " + utils.surfaces.size());

			utils.origPoints = new ConcurrentHashMap<Integer, MyPickablePoint>();
			for (MyPickablePoint mp : utils.points.values()) {
				utils.origPoints.put(new Integer(mp.id), mp.clone());
			}

			System.out.println("Points: " + utils.points.size());
			System.out.println("Edges: " + utils.edges.size());
			System.out.println("Surfaces: " + utils.surfaces.size());

			// Creates the 3d object
			System.out.println("Thread: " + Thread.currentThread().getName());
			chart.getAxeLayout().setXAxeLabel("X");
			chart.getAxeLayout().setYAxeLabel("Y");
			chart.getAxeLayout().setZAxeLabel("Z");
			chart.getView().setSquared(false);

			// pauseAnimator();
			// resumeAnimator();
			// SurfaceDemo.instance.canvas.getAnimator().start();

			initDraw();
			System.out.println("Thread: " + Thread.currentThread().getName());

			canvas.addKeyListener(new KeyListener() {

				@Override
				public void keyReleased(KeyEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void keyPressed(KeyEvent e) {
					// TODO Auto-generated method stub
					// System.out.println(e.getKeyCode());
					if (e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_1) {
						double angleDelta = 0;
						if (e.getKeyCode() == KeyEvent.VK_0) {
							angleDelta = 1;
						}
						if (e.getKeyCode() == KeyEvent.VK_1) {
							angleDelta = -1;
						}
						SurfaceDemo.instance.utils.rotatePoints(angleDelta, false);
						System.out.println("angle=" + angleTxt);
					}
				}

				@Override
				public void keyTyped(KeyEvent e) {
					// TODO Auto-generated method stub

				}
			});

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void splitLongEdges(int pointId, int edgeNo) {
		// splitLongEdges
		ArrayList<MyPickablePoint> sortedXList = new ArrayList(SurfaceDemo.instance.utils.points.values());
		Collections.sort(sortedXList, new MyPickablePointXComparator());
		ArrayList<MyPickablePoint> sortedZList = new ArrayList(SurfaceDemo.instance.utils.points.values());
		Collections.sort(sortedZList, new MyPickablePointZComparator());

		float minX = sortedXList.get(0).getX();
		float maxX = sortedXList.get(sortedXList.size() - 1).getX();
		float minZ = sortedZList.get(0).getZ();
		float maxZ = sortedZList.get(sortedZList.size() - 1).getZ();

		ArrayList<MyEdge> edgesToRemove = new ArrayList<MyEdge>();

		Iterator<MyEdge> edgeIt = utils.edges.values().iterator();
		while (edgeIt.hasNext()) {
			MyEdge edge = edgeIt.next();
			if (edge.edgeNo == 19) {
				System.out.println();
			}
			if (edge.length >= (maxX - minX) * 0.5 || edge.length >= (maxZ - minZ) * 0.5) {
				// need to split edge - create 2 edges instead onež
				System.out.println(edge.length);
				MyPickablePoint p1 = SurfaceDemo.instance.utils.points.get(edge.points.get(0));
				MyPickablePoint p2 = SurfaceDemo.instance.utils.points.get(edge.points.get(1));

				float x = (p1.getX() + p2.getX()) / 2;
				float y = (p1.getY() + p2.getY()) / 2;
				float z = (p1.getZ() + p2.getZ()) / 2;

				Coord3d p = new Coord3d(x, y, z);
				int id = pointId++;
				MyPickablePoint newP = utils.createOrFindMyPickablePoint(id, p, -1);

				MyEdge edge1 = new MyEdge(edgeNo, -1);
				edge1.addPoint(p1.id);
				edge1.addPoint(newP.id);
				MySurface surface = utils.surfaces.get(edge.surfaceNo);
				if (surface == null) {
					surface = new MySurface(edge.surfaceNo);
					utils.surfaces.put(edge.surfaceNo, surface);
				}
				surface.addEdge(edge1);
				utils.edges.put(edgeNo, edge1);
				edgeNo++;

				MyEdge edge2 = new MyEdge(edgeNo, -1);
				edge2.addPoint(newP.id);
				edge2.addPoint(p2.id);
				surface.addEdge(edge2);
				utils.edges.put(edgeNo, edge2);
				edgeNo++;

				edgesToRemove.add(edge);
				System.out.println("Points used: " + p1.id + " " + newP.id + " " + p2.id);
			}
		}
		edgeIt = utils.edges.values().iterator();
		while (edgeIt.hasNext()) {
			MyEdge edge = edgeIt.next();
			if (edgesToRemove.contains(edge)) {
				edgeIt.remove();
			}
		}
	}

	// private void enablePickingTexts(ArrayList<PickableDrawableTextBitmap>
	// edgeTexts, Chart chart, int brushSize) {
	// AWTMousePickingPan2dController<?, ?> mousePicker =
	// (AWTMousePickingPan2dController) chart.getFactory()
	// .newPickingController(chart);
	// PickingSupport picking = mousePicker.getPickingSupport();
	//
	// for (PickableDrawableTextBitmap txt : edgeTexts) {
	// picking.registerDrawableObject(txt, txt);
	// }
	//
	// picking.addObjectPickedListener(new IObjectPickedListener() {
	// @Override
	// public void objectPicked(List<?> picked, final PickingSupport ps) {
	// if (picked.size() > 0) // && (System.currentTimeMillis() -
	// // camMouse.clickTimeMillis < 200)) {
	// {
	// // System.out.println("Size: " + picked.size());
	// // for (int i = 0; i < picked.size(); i++) {
	// if (picked.get(0) instanceof PickableDrawableTextBitmap) {
	//
	// final PickableDrawableTextBitmap txt = (PickableDrawableTextBitmap)
	// picked.get(0);
	//
	// JPopupMenu edgeMenu = new JPopupMenu();
	// JMenuItem menuItem = new JMenuItem("Delete edge " + txt.getText());
	// menuItem.addActionListener(new ActionListener() {
	// @Override
	// public void actionPerformed(ActionEvent arg0) {
	// SurfaceDemo.instance.utils.edges.remove(Integer.valueOf(txt.getText()));
	// SurfaceDemo.instance.utils.edgeTexts.remove(txt);
	// SurfaceDemo.instance.initDraw();
	// ps.unRegisterPickableObject(txt);
	// }
	// });
	// edgeMenu.add(menuItem);
	// edgeMenu.show(SurfaceDemo.instance.canvas, 100, 100);
	// }
	// // }
	// }
	// }
	// });
	// }

	public void initDraw() {
		// instance.canvas.getAnimator().stop();
		clearPicking();
		instance.getChart().getScene().getGraph().remove(instance.myComposite);
		myComposite = new MyComposite();
		myComposite.clear();
		addAxis();
		addCurrentRotation();
		utils.edgeTexts = new ArrayList<PickableDrawableTextBitmap>();
		utils.pointTexts = new ArrayList<DrawableTextBitmap>();
		myComposite.add(new ArrayList<MyPickablePoint>(utils.points.values()));
		ArrayList<Integer> alreadyAddedPointsText = new ArrayList<Integer>();
		for (MyEdge edge : utils.edges.values()) {
			LineStrip ls = new LineStrip();
			ls.setWidth(2f);
			ls.setWireframeColor(Color.GRAY);
			edge.setLineStrip(ls);
			if (NUMBER_EDGES) {
				PickableDrawableTextBitmap t5 = new PickableDrawableTextBitmap(String.valueOf(edge.edgeNo), edge.center,
						Color.BLUE);
				t5.setHalign(Halign.CENTER); // TODO: invert
				t5.setValign(Valign.CENTER); // TODO: invert
				// t5.setValign(Valign.BOTTOM); // TODO: invert
				// left/right
				t5.setPickingId(edge.edgeNo);
				utils.edgeTexts.add(t5);
			}
			for (Integer pointNo : edge.points) {
				MyPickablePoint point = SurfaceDemo.instance.utils.points.get(pointNo);
				ls.add(utils.points.get(point.id));
				if (SurfaceDemo.NUMBER_POINTS) {
					if (!alreadyAddedPointsText.contains(pointNo)) {
						DrawableTextBitmap t4 = new DrawableTextBitmap(String.valueOf(point.id), point.xyz, Color.BLACK);
						t4.setHalign(Halign.CENTER); // TODO: invert
						t4.setValign(Valign.CENTER); // TODO: invert
						// left/right
						utils.pointTexts.add(t4);
						alreadyAddedPointsText.add(pointNo);
					}
				}
			}
			
			//centers of continuous edges
			for (MyEdge continuousEdge : utils.continuousEdges.values()) {
				Point edgeCenter = new Point(continuousEdge.center,Color.RED,3.0f);
				myComposite.add(edgeCenter);
			}
			
			if (plasma == null) {
				plasma = new Sphere(new Coord3d(0, 0, 0), 0.4f, 4, Color.BLUE);
				plasma.setWireframeColor(Color.BLUE);
			} else
				plasma.setPosition(plasma.getPosition());
			instance.myComposite.add(plasma);

			myComposite.add(ls);
		}

		if (NUMBER_EDGES) {
			myComposite.add(utils.edgeTexts);
			instance.enablePickingTexts(instance.utils.edgeTexts, instance.chart, 10);
		}
		if (NUMBER_POINTS) {
			myComposite.add(utils.pointTexts);
		}
		instance.getChart().getScene().getGraph().add(instance.myComposite);
		instance.enablePicking(instance.utils.points.values(), instance.chart, 10);
		// instance.canvas.getAnimator().start();
		System.out.println("Composite element size: " + myComposite.getDrawables().size());
	}

	private void clearPicking() {

		for (AbstractCameraController controller : chart.getControllers()) {
			if (controller instanceof AWTMousePickingController) {
				AWTMousePickingController mousePicker = (AWTMousePickingController) controller;
				mousePicker.getPickingSupport().unRegisterAllPickableObjects();
				break;
			}
		}
	}

	private void enablePicking(Collection<MyPickablePoint> points, Chart chart, int brushSize) {

		if (getPickingSupport() != null) {
			for (MyPickablePoint p : points) {
				getPickingSupport().registerPickableObject(p, p);
			}

			getPickingSupport().addObjectPickedListener(new IObjectPickedListener() {
				@Override
				public void objectPicked(List<?> picked, PickingSupport ps) {

					if (picked.size() > 0) // && (System.currentTimeMillis() -
					{
						if (picked.get(0).getClass().getName().equals("com.kz.grbl.MyPickablePoint")) {
							MyPickablePoint mp = ((MyPickablePoint) picked.get(0));
							lastClickedPoint = mp;
							float offset = Float.valueOf(Settings.instance.getSetting("plasma_pierce_offset_mm"));
							move(mp,false,offset);
							
							Point p = SurfaceDemo.instance.utils.calculateOffsetPoint(mp);
							p.setColor(Color.GREEN );
							p.setWidth(6.0f);
							
							myComposite.add(p);
							
							System.out.println(mp.toString());
						} else if (picked.get(0).getClass().getName()
								.equals("org.jzy3d.plot3d.primitives.pickable.PickablePolygon")) {
						} else {
						}

					}
				}
			});
		}
	}

	public void addAxis() {
		LineStrip yAxis = new LineStrip();
		yAxis.setWireframeColor(Color.GREEN);
		yAxis.add(new Point(new Coord3d(0, 0, 0)));
		yAxis.add(new Point(new Coord3d(0, 1, 0)));
		DrawableTextBitmap yAxisTxt = new DrawableTextBitmap("y", new Coord3d(0, 1, 0), Color.GREEN);
		yAxisTxt.setHalign(Halign.CENTER); // TODO: invert
		yAxisTxt.setValign(Valign.CENTER); // TODO: invert
		myComposite.add(yAxisTxt);

		LineStrip xAxis = new LineStrip();
		xAxis.setWireframeColor(Color.BLUE);
		xAxis.add(new Point(new Coord3d(0, 0, 0)));
		xAxis.add(new Point(new Coord3d(0.5, 0, 0)));
		DrawableTextBitmap xAxisTxt = new DrawableTextBitmap("x", new Coord3d(0.3, 0, 0), Color.BLUE);
		xAxisTxt.setHalign(Halign.CENTER); // TODO: invert
		xAxisTxt.setValign(Valign.CENTER); // TODO: invert
		myComposite.add(xAxisTxt);

		LineStrip zAxis = new LineStrip();
		zAxis.setWireframeColor(Color.RED);
		zAxis.add(new Point(new Coord3d(0, 0, 0)));
		zAxis.add(new Point(new Coord3d(0, 0, 0.5)));
		myComposite.add(xAxis);
		myComposite.add(zAxis);
		myComposite.add(yAxis);
		DrawableTextBitmap zAxisTxt = new DrawableTextBitmap("z", new Coord3d(0, 0, 0.3), Color.RED);
		zAxisTxt.setHalign(Halign.CENTER); // TODO: invert
		zAxisTxt.setValign(Valign.CENTER); // TODO: invert
		myComposite.add(zAxisTxt);
	}

	public void addCurrentRotation() {
		LineStrip currentRot = null;

		currentRot = new LineStrip();
		currentRot.setWidth(0.3f);
		currentRot.setWireframeColor(Color.BLUE);
		currentRot.add(new Point(new Coord3d(0, 0, 0)));
		rotationPoint = new Coord3d();

		currentRot.add(new Point(rotationPoint));

		currentRotTxt = new DrawableTextBitmap("0.0", rotationPoint, Color.BLUE);
		currentRotTxt.setHalign(Halign.CENTER); // TODO: invert
		currentRotTxt.setValign(Valign.CENTER); // TODO: invert
		myComposite.add(currentRot);
		myComposite.add(currentRotTxt);
		float value = Float.valueOf(SurfaceDemo.instance.angleTxt).floatValue();
		calculateRotationPoint(value);

	}

	public void calculateRotationPoint(double angle) {
		float length = 5;
		float x = length * (float) Math.sin(Math.toRadians(angle));
		float z = length * (float) Math.cos(Math.toRadians(angle));
		rotationPoint.set(x, 0, z);
		currentRotTxt.setText(String.format("%.2f", angle));
	}

	public void pauseAnimator() {
		instance.getChart().pauseAnimator();
	}

	public void resumeAnimator() {
		instance.getChart().resumeAnimator();
	}

	public void move(MyPickablePoint tempPoint, boolean cut, float offset) {
		// if (cylinder == null) {
		// cylinder = new Cylinder(tempPoint);

		// }
		// cylinder.move(tempPoint);
		if (cylinderPoint == null)
			cylinderPoint = new Point();

		if (cut) {
			plasma.setColor(Color.RED);
			plasma.setWireframeColor(Color.RED);
		} else {
			plasma.setColor(Color.BLUE);
			plasma.setWireframeColor(Color.BLUE);
		}

		cylinderPoint.setCoord(tempPoint.xyz);
		plasma.setPosition(cylinderPoint.xyz.add(new Coord3d(0,0,offset)) );

		String gcode = SurfaceDemo.instance.utils.coordinateToGcode(tempPoint);
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("prog.gcode", true)))) {
			if (cut)
				out.println(String.format("G1 %s A%s B%s (pointId: %d)", gcode, SurfaceDemo.instance.angleTxt, SurfaceDemo.instance.angleTxt, tempPoint.id));
			else
				out.println(String.format("G0 %s A%s B%s (pointId: %d)", gcode, SurfaceDemo.instance.angleTxt, SurfaceDemo.instance.angleTxt, tempPoint.id));
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (ZOOM_POINT) {
			float edge = canvas.getView().getBounds().getXmax() - canvas.getView().getBounds().getXmin();
			canvas.getView().setBoundManual(new BoundingBox3d(plasma.getPosition(), edge));
		}

		// instance.getChart().render();
		try {
			Thread.sleep(Cylinder.sleep);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void moveAbove(MyPickablePoint tempPoint, float offset, long pierceTimeMs) {
		Coord3d abovePoint= tempPoint.xyz.add(0f,0f,offset);
		plasma.setPosition(abovePoint);

		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("prog.gcode", true)))) {
			String gcode = SurfaceDemo.instance.utils.coordinateToGcode(abovePoint,offset);
			plasma.setColor(Color.BLUE);
			plasma.setWireframeColor(Color.BLUE);
			try
			{
				Thread.sleep(Long.valueOf(1000));
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
			out.println("G0 " + gcode);
			out.println("G1 " + gcode);
			plasma.setColor(Color.RED);
			plasma.setWireframeColor(Color.RED);
			out.println("G4 P" + pierceTimeMs);
			try
			{
				Thread.sleep(Long.valueOf(pierceTimeMs));
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (ZOOM_POINT) {
			float edge = canvas.getView().getBounds().getXmax() - canvas.getView().getBounds().getXmin();
			canvas.getView().setBoundManual(new BoundingBox3d(plasma.getPosition(), edge));
		}

		// instance.getChart().render();
		try {
			Thread.sleep(Cylinder.sleep);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
