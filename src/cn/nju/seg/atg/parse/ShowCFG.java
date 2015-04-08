package cn.nju.seg.atg.parse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.Filter;
import org.eclipse.zest.layouts.LayoutItem;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import cn.nju.seg.atg.visitor.CFGNode;

/**
 * 以图形化方式显示当前函数的AST树
 * @author ChengXin
 */
public class ShowCFG {
	private static List<Integer> nodeShowed = new ArrayList<Integer>();
	private static Display d = Display.getDefault();
	private static Shell shell = new Shell(d);
	private static Graph g = new Graph(shell, SWT.NONE);
	private static GraphConnection gc = null;
	private static GraphNode[] graphNodes = null;
	private final int NODE_NUMBER = 100;
	private static CFGNode startNode = null;
	private final static int WIDTH = 800;
	private final static int HEIGHT = 800;
	
	public ShowCFG(CFGNode startNode){
		this.setStartNode(startNode);
	}
	
	public CFGNode getStartNode() {
		return startNode;
	}

	public void setStartNode(CFGNode startNode) {
		ShowCFG.startNode = startNode;
	}

	public void showCFG(final String cfgPicPath){
		nodeShowed = new ArrayList<Integer>();
		shell = new Shell(d);
		g = new Graph(shell, SWT.NONE);
		graphNodes = new GraphNode[NODE_NUMBER];
		
		shell.setText("CFG的树形结构");
		shell.setLayout(new FillLayout());
		shell.setSize(WIDTH, HEIGHT);
		
//		new GraphNode(g, SWT.NONE, "保存为PNG");
		
		graphNodes[0] = new GraphNode(g, SWT.NONE, "1"+getNodeSymbol(this.getStartNode()));
		drawCFG(this.getStartNode());
		
		TreeLayoutAlgorithm treeLayoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
		Filter filter = new Filter() {
			public boolean isObjectFiltered(LayoutItem item) {
				// Get the "Connection" from the Layout Item
				// and use this connection to get the "Graph Data"
				Object object = item.getGraphData();
				if  (object instanceof GraphConnection ) {
					GraphConnection connection = (GraphConnection) object;
					if ( connection.getData() != null && connection.getData() instanceof Boolean ) {
						// If the data is false, don't filter, otherwise, filter.
						return ((Boolean)connection.getData()).booleanValue();
					}
					return false;
				}
				return false;
			}
		};
		treeLayoutAlgorithm.setFilter(filter);
		g.setLayoutAlgorithm(treeLayoutAlgorithm, true);
		g.addDisposeListener(
			new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent arg0) {
//					saveCFG(cfgPicPath);
				}
			}
		);
//		g.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				if (((Graph) e.widget).getSelection().toString().compareTo("[GraphModelNode: 保存为PNG]") == 0) {
//					saveCFG();
//				}
//			}
//		});

		shell.open();
	}
	
	public void showCFG(){
		nodeShowed = new ArrayList<Integer>();
		shell = new Shell(d);
		g = new Graph(shell, SWT.NONE);
		graphNodes = new GraphNode[NODE_NUMBER];
		
		shell.setText("CFG的树形结构");
		shell.setLayout(new FillLayout());
		shell.setSize(WIDTH, HEIGHT);
		
//		new GraphNode(g, SWT.NONE, "保存为PNG");
		
		graphNodes[0] = new GraphNode(g, SWT.NONE, "1"+getNodeSymbol(this.getStartNode()));
		drawCFG(this.getStartNode());
		
		TreeLayoutAlgorithm treeLayoutAlgorithm = new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING);
		Filter filter = new Filter() {
			public boolean isObjectFiltered(LayoutItem item) {
				// Get the "Connection" from the Layout Item
				// and use this connection to get the "Graph Data"
				Object object = item.getGraphData();
				if  (object instanceof GraphConnection ) {
					GraphConnection connection = (GraphConnection) object;
					if ( connection.getData() != null && connection.getData() instanceof Boolean ) {
						// If the data is false, don't filter, otherwise, filter.
						return ((Boolean)connection.getData()).booleanValue();
					}
					return false;
				}
				return false;
			}
		};
		treeLayoutAlgorithm.setFilter(filter);
		g.setLayoutAlgorithm(treeLayoutAlgorithm, true);
		g.addDisposeListener(
			new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent arg0) {
//					saveCFG(cfgPicPath);
				}
			}
		);
//		g.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				if (((Graph) e.widget).getSelection().toString().compareTo("[GraphModelNode: 保存为PNG]") == 0) {
//					saveCFG();
//				}
//			}
//		});

		shell.open();
	}
	
	private static void drawCFG(CFGNode currentNode){
		if(currentNode != null){
			if(currentNode.getOffset() != -1 && !nodeShowed.contains(currentNode.getOffset())){
				nodeShowed.add(currentNode.getOffset());
				if(currentNode.getChildren() != null){
					List<CFGNode> children = currentNode.getChildren();
					if(children.size() > 0){
						for(int i=0; i<children.size(); i++){
							if(graphNodes[children.get(i).getNodeNumber()-1] == null){
								graphNodes[children.get(i).getNodeNumber()-1] = new GraphNode(g, SWT.NONE, children.get(i).getNodeNumber()+getNodeSymbol(children.get(i)));
							}
							if(currentNode.getOffset() <= children.get(i).getOffset()){
								gc = new GraphConnection(g, ZestStyles.CONNECTIONS_DIRECTED, graphNodes[currentNode.getNodeNumber()-1], graphNodes[children.get(i).getNodeNumber()-1]);
								gc.setLineColor(ColorConstants.black);
								gc.setLineWidth(2);
							}else{
								gc = new GraphConnection(g, ZestStyles.CONNECTIONS_DIRECTED, graphNodes[currentNode.getNodeNumber()-1], graphNodes[children.get(i).getNodeNumber()-1]);
								gc.setLineColor(ColorConstants.red);
								gc.setLineWidth(2);
								gc.setData(Boolean.TRUE);
							}
						}
					}
				}
				if(currentNode.getIfChild() != null){
					if(graphNodes[currentNode.getIfChild().getNodeNumber()-1] == null){
						graphNodes[currentNode.getIfChild().getNodeNumber()-1] = new GraphNode(g, SWT.NONE, currentNode.getIfChild().getNodeNumber()+getNodeSymbol(currentNode.getIfChild()));
					}
					if(currentNode.getOffset() <= currentNode.getIfChild().getOffset()){
						gc = new GraphConnection(g, ZestStyles.CONNECTIONS_DIRECTED, graphNodes[currentNode.getNodeNumber()-1], graphNodes[currentNode.getIfChild().getNodeNumber()-1]);
						gc.setLineColor(ColorConstants.black);
						gc.setLineWidth(2);
					}else{
						gc = new GraphConnection(g, ZestStyles.CONNECTIONS_DIRECTED, graphNodes[currentNode.getNodeNumber()-1], graphNodes[currentNode.getIfChild().getNodeNumber()-1]);
						gc.setLineColor(ColorConstants.red);
						gc.setLineWidth(2);
						gc.setData(Boolean.TRUE);
					}
				}
				if(currentNode.getElseChild() != null){
					if(graphNodes[currentNode.getElseChild().getNodeNumber()-1] == null){
						graphNodes[currentNode.getElseChild().getNodeNumber()-1] = new GraphNode(g, SWT.NONE, currentNode.getElseChild().getNodeNumber()+getNodeSymbol(currentNode.getElseChild()));
					}
					if(currentNode.getOffset() <= currentNode.getElseChild().getOffset()){
						gc = new GraphConnection(g, ZestStyles.CONNECTIONS_DIRECTED, graphNodes[currentNode.getNodeNumber()-1], graphNodes[currentNode.getElseChild().getNodeNumber()-1]);
						gc.setLineColor(ColorConstants.black);
						gc.setLineWidth(2);
					}else{
						gc = new GraphConnection(g, ZestStyles.CONNECTIONS_DIRECTED, graphNodes[currentNode.getNodeNumber()-1], graphNodes[currentNode.getElseChild().getNodeNumber()-1]);
						gc.setLineColor(ColorConstants.red);
						gc.setLineWidth(2);
						gc.setData(Boolean.TRUE);
					}
				}
			}
			
			if(currentNode.getChildren() != null){
				List<CFGNode> children = currentNode.getChildren();
				if(children.size() > 0){
					for(int i=0; i<children.size(); i++){
						if(!nodeShowed.contains(children.get(i).getOffset())){
							drawCFG(children.get(i));
						}
					}
				}
			}
			if(currentNode.getIfChild() != null && !nodeShowed.contains(currentNode.getIfChild().getOffset())){
				drawCFG(currentNode.getIfChild());
			}
			if(currentNode.getElseChild() != null && !nodeShowed.contains(currentNode.getElseChild().getOffset())){
				drawCFG(currentNode.getElseChild());
			}
		}
	}
	
//	private static void saveCFG(String cfgPicPath) {
//		int width = g.getBounds().width;
//		int height = g.getBounds().height;
//		IFigure figure = g.getContents();		
//		Image img = new Image(null, width, height);
//		GC gc = new GC(img);
//		Graphics g = new SWTGraphics(gc);
//		g.translate(figure.getBounds().getLocation());
//		figure.paint(g);
//		g.dispose();
//		gc.dispose();
//		ImageLoader imgLoader = new ImageLoader();
//		imgLoader.data = new ImageData[] { img.getImageData() };
//		imgLoader.save(cfgPicPath, SWT.IMAGE_PNG);
//	}
	
	private static String getNodeSymbol(CFGNode node){
		String nodeSymbol = "";
		int sign = node.getSign();
		switch(sign){
			case 0: nodeSymbol = ": "+node.getOperator(); break;
			case 1:	nodeSymbol = ": "+node.getOperator(); break;
			case 2: nodeSymbol = ": for block"; break;
			case 3: nodeSymbol = ": "+node.getBinaryExpression().getRawSignature(); break;
			case 4: nodeSymbol = ": return"; break;
			case 5: nodeSymbol = ": while block"; break;
			case 6: nodeSymbol = ": do-while block"; break;
			case 7: nodeSymbol = ": break"; break;
			case 8: nodeSymbol = ": continue"; break;
			case -1: nodeSymbol = "";
			default: nodeSymbol="";
		}
		return nodeSymbol;
	}
}
