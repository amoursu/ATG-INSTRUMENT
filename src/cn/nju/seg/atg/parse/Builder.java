package cn.nju.seg.atg.parse;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTSignatureUtil;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import cn.nju.seg.atg.visitor.CFGNode;
import cn.nju.seg.atg.visitor.FunctionVisitor;

/**
 * 
 * @author ChengXin
 * @author zy
 * 
 */
@SuppressWarnings("deprecation")
public class Builder extends AbstractAST {
	/**
	 * 需要调用的action的名字
	 */
	private String actionName;

	/**
	 * 执行单元测试的函数名
	 */
	public static String funcName = null;
	
	/**
	 * 由函数转成的CFG树的开始节点
	 */
	public static CFGNode cfgStartNode = null;
	
	/**
	 * CFG树中的当前节点，是全局变量，在构建、遍历CFG树的时候会用到
	 */
	public static CFGNode currentNode = null;
	
	/**
	 * CFG树中节点的编号，是全局变量，构建CFG树时会用到
	 */
	public static int nodeNumber = -1;

	/**
	 * 构建CFG树过程中用于存放终止节点的链表，是全局变量
	 */
	public static List<CFGNode> terminalNodes = new ArrayList<CFGNode>();
	/**
	 * 构建CFG树过程中用于存放break节点的链表
	 */
    public static List<CFGNode> breakNodes = new ArrayList<CFGNode>();
    /**
	 * 构建CFG树过程中用于存放continue节点的链表
	 */
    public static List<CFGNode> continueNodes = new ArrayList<CFGNode>();
    
	/**
	 * 用于存放已打印节点的链表，CFG树输出是会用到
	 */
	public static List<Integer> nodePrinted = new ArrayList<Integer>();

	/**
	 * 当前Eclispe实例的console流
	 */
	public static MessageConsoleStream consoleStream;	

	/**
	 * 带参构造函数
	 * @param actionName
	 */
	public Builder(String actionName)
	{
		this.actionName = actionName;
	}
	
	@Override
	public void run(IFunctionDeclaration ifd) {
		
/************************************************
 | 获取当前"测试Eclipse"的Console窗口				|
 | 将其命名为"CLF Console"，为输出运行结果做准备		|
 ***********************************************/
		//定义一个console窗口
		MessageConsole console = new MessageConsole("ATG Console",null);
		//将console窗口添加到当前测试Eclipse中
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{console});
		//获取console的信息流consoleStream，为输出运行结果做准备
		consoleStream = console.newMessageStream();
		//在当前测试Eclipse中显示该console的内容
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);		
		
/************************************************
 | 借助CDT插件，遍历函数节点ifd，生成CFG树			|
 ***********************************************/
		IASTTranslationUnit iatu = parse(ifd);
		cfgStartNode = new CFGNode();
		currentNode = new CFGNode();
		nodeNumber = 0;
		FunctionVisitor expressionVisitor = new FunctionVisitor();
		this.setParsed(expressionVisitor, iatu);
		
/************************************************
 | 从CFG树的条件节点中取出逻辑符号					|
 | 并记录到该条件节点中                         	|
 ***********************************************/
		nodePrinted = new ArrayList<Integer>();
		addSymbol(cfgStartNode);		
		
/************************************************
 | 2012.10.24								    |
 | 利用zest插件，显示CFG的树形结构			    	|
 ***********************************************/
		if (this.actionName.equals("cfg"))
		{		
			ShowCFG showcfg = new ShowCFG(cfgStartNode);
			showcfg.showCFG();
		}
		
/************************************************
 | 插桩程序										|
 | 在同目录下生成插桩后源码，例如a.cpp插桩后为a+.cpp	|
 ***********************************************/
		if(this.actionName.equals("instrument")){
		String fileRead = ifd.getLocationURI().toString().substring("file:".length()).replace("%20", " ");
		String fileWritten = ifd.getLocationURI().toString().substring("file:".length()).replace("%20", " ").replace(".", "+.");
		try {
			RandomAccessFile raf = new RandomAccessFile(fileRead, "r");
			FileWriter writer = new FileWriter(fileWritten);
			raf.seek(0);
			byte[] bytes = new byte[(int)raf.length()];
			raf.read(bytes);
			writer.write(new String(bytes));
			raf.close();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Instrument inst = new Instrument(cfgStartNode);
		inst.instrumantation(fileWritten);		
		}
	}
	
	/**
	 * 从CFG树的条件节点中取出逻辑符号，并记录到该条件节点中
	 * @param currentNode
	 */
	public void addSymbol(CFGNode currentNode){
		if (currentNode != null) {
			if (currentNode.getOffset() != -1 && !nodePrinted.contains(currentNode.getOffset())) {
				if (currentNode.getBinaryExpression() != null) {
					IASTBinaryExpression iabe = currentNode.getBinaryExpression();
					String operator = ASTSignatureUtil.getBinaryOperatorString(iabe);
					if(operator.compareTo(">")==0){
						if(currentNode.getIfChild() != null){
							currentNode.getIfChild().setOperator(">");
						}
						if(currentNode.getElseChild() != null){
							currentNode.getElseChild().setOperator("<=");
						}
					}else if(operator.compareTo(">=")==0){
						if(currentNode.getIfChild() != null){
							currentNode.getIfChild().setOperator(">=");
						}
						if(currentNode.getElseChild() != null){
							currentNode.getElseChild().setOperator("<");
						}
					}else if(operator.compareTo("<")==0){
						if(currentNode.getIfChild() != null){
							currentNode.getIfChild().setOperator("<");
						}
						if(currentNode.getElseChild() != null){
							currentNode.getElseChild().setOperator(">=");
						}
					}else if(operator.compareTo("<=")==0){
						if(currentNode.getIfChild() != null){
							currentNode.getIfChild().setOperator("<=");
						}
						if(currentNode.getElseChild() != null){
							currentNode.getElseChild().setOperator(">");
						}
					}else if(operator.compareTo("==")==0){
						if(currentNode.getIfChild() != null){
							currentNode.getIfChild().setOperator("==");
						}
						if(currentNode.getElseChild() != null){
							currentNode.getElseChild().setOperator("!=");
						}
					}else if(operator.compareTo("!=")==0){
						if(currentNode.getIfChild() != null){
							currentNode.getIfChild().setOperator("!=");
						}
						if(currentNode.getElseChild() != null){
							currentNode.getElseChild().setOperator("==");
						}
					}
				}
			}
			nodePrinted.add(currentNode.getOffset());
			List<CFGNode> children = currentNode.getChildren();
			if (children != null) {
				for (int i = 0; i < children.size(); i++) {
					if (!nodePrinted.contains(children.get(i).getOffset())) {
						addSymbol(children.get(i));
					}
				}
			}
			addSymbol(currentNode.getIfChild());
			addSymbol(currentNode.getElseChild());
		}
	}

	/**
	 * 设置CDT插件的遍历参数，并且遍历AST
	 * @param v
	 * @param iatu
	 */
	private void setParsed(ASTVisitor v, IASTTranslationUnit iatu) {
		v.shouldVisitStatements = true;
		v.includeInactiveNodes = true;
		v.shouldVisitAmbiguousNodes = true;
		v.shouldVisitArrayModifiers = true;
		v.shouldVisitBaseSpecifiers = true;
		v.shouldVisitDeclarations = true;
		v.shouldVisitDeclarators = true;
		v.shouldVisitDeclSpecifiers = true;
		v.shouldVisitDesignators = true;
		v.shouldVisitEnumerators = true;
		v.shouldVisitExpressions = true;
		v.shouldVisitImplicitNameAlternates = true;
		v.shouldVisitImplicitNames = true;
		v.shouldVisitInitializers = true;
		v.shouldVisitNames = true;
		v.shouldVisitNamespaces = true;
		v.shouldVisitParameterDeclarations = true;
		v.shouldVisitPointerOperators = true;
		v.shouldVisitProblems = true;
		v.shouldVisitTemplateParameters = true;
		v.shouldVisitTranslationUnit = true;
		v.shouldVisitTypeIds = true;
		iatu.accept(v);
	}
}
