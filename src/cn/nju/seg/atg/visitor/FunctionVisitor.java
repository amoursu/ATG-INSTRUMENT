package cn.nju.seg.atg.visitor;

import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.internal.core.model.ASTStringUtil;

import cn.nju.seg.atg.parse.Builder;

/**
 * 
 * @author ChengXin
 * @author zy
 *
 */
@SuppressWarnings("restriction")
public class FunctionVisitor extends ASTVisitor {
	
	private ArrayList<String> parameters;
	
	public FunctionVisitor() {
		
	}
	
	public String[] getParameters()
	{
		String[] tmps = new String[this.parameters.size()];
		for(int i=0;i<this.parameters.size();i++)
		{
			tmps[i] = String.valueOf(this.parameters.get(i));
		}
		return tmps;
	}
	
	public int visit(IASTDeclarator node) {
		if(node instanceof IASTStandardFunctionDeclarator){
			IASTStandardFunctionDeclarator iasfd = (IASTStandardFunctionDeclarator)node;
			String funcName = iasfd.getName().getRawSignature();
			//System.out.println("function name:" + funcName);
			String[] parameters = ASTStringUtil.getParameterSignatureArray(iasfd);
			String parameter = "(";
			for(int i=0;i<parameters.length;i++)
			{
				parameter = parameter.concat(parameters[i]);
				if(i < parameters.length-1)
					parameter = parameter + ", ";
			}
			parameter = parameter.concat(")");
//			System.out.println("parameter:" + parameter);
			funcName = funcName + parameter;
			IASTParameterDeclaration[] iParameters = iasfd.getParameters();
			this.parameters = new ArrayList<String>();
			for(int i=0;i<iParameters.length;i++)
			{
//				System.out.print(iParameters[i].getDeclarator().getName().getRawSignature() + "   ");
//				System.out.println(iParameters[i].getDeclarator().getRawSignature());
				if(iParameters[i].getDeclarator().getName().getRawSignature().equals(iParameters[i].getDeclarator().getRawSignature()))
				{
					this.parameters.add(iParameters[i].getDeclarator().getRawSignature());
//					System.out.println("parameter:" + this.parameters.get(this.parameters.size()-1));
				}
			}
			
			if(funcName.compareTo(Builder.funcName)==0){
				/*
				 * 获取函数的输入参数的类型、参数个数
				 */
//				Builder.parameterTypes = ASTStringUtil.getParameterSignatureArray(iasfd);
//				ICLFF_ATG.NUM_OF_PARAM = Builder.parameterTypes.length;
				int lastStatementOffset = -1;
				int firstSequenceStatementIndex = -1;
				int endStatementIndex = -1;
				
				IASTCompoundStatement iacs = (IASTCompoundStatement)iasfd.getParent().getChildren()[2];				
				IASTNode[] iacsChildren = iacs.getChildren();
				int n = iacsChildren.length;
				
				if(n>0){
					if(iacsChildren[0] instanceof IASTIfStatement){ }            //if
					else if(iacsChildren[0] instanceof IASTForStatement){ }      //for
					else if(iacsChildren[0] instanceof IASTWhileStatement){ }    //while
					else if(iacsChildren[0] instanceof IASTDoStatement){ }       //do
					else{
						int offsetTmp = iacsChildren[0].getFileLocation().getNodeOffset();
						Builder.cfgStartNode.setSign(-1);
						Builder.cfgStartNode.setOffset(offsetTmp);
						Builder.nodeNumber++;
						Builder.cfgStartNode.setNodeNumber(Builder.nodeNumber);
					}
				}
				
				int countI = 0;
				for(int i=0; i<n; i++){
					/****************************
					* 		If-Else语句			*
					****************************/
					if(iacsChildren[i] instanceof IASTIfStatement){						
						countI++;
						endStatementIndex = i;						
						if(countI == 1){
							lastStatementOffset = iacsChildren[i].getFileLocation().getNodeOffset();
							if(i==0){
								Builder.cfgStartNode = Builder.currentNode;
							}
							else{
								Builder.cfgStartNode.addChild(Builder.currentNode);
							    Builder.currentNode.addParent(Builder.cfgStartNode);
							}

							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							iacsChildren[i].accept(ifStatementVisitor);
							
							firstSequenceStatementIndex = i+1;
						}else{
							lastStatementOffset = iacsChildren[i].getFileLocation().getNodeOffset();
							
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = iacsChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=0; k<terminalNodesNum; k++){
									Builder.terminalNodes.get(k).addChild(nextNode);
									nextNode.addParent(Builder.terminalNodes.get(k));
								}
								Builder.terminalNodes = new ArrayList<CFGNode>();
								
								Builder.currentNode = new CFGNode();
								Builder.currentNode.addParent(nextNode);
								nextNode.addChild(Builder.currentNode);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=0; k<terminalNodesNum; k++){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
								}
								Builder.terminalNodes = new ArrayList<CFGNode>();
							}
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							iacsChildren[i].accept(ifStatementVisitor);
							
							firstSequenceStatementIndex = i+1;
						}
					}else if(iacsChildren[i] instanceof IASTForStatement){
						/****************************
						* 			For语句			*
						****************************/
						countI++;
						endStatementIndex = i;
						CFGNode forNode;
						if(countI == 1){
							lastStatementOffset = iacsChildren[i].getFileLocation().getNodeOffset();
							
							if(i==0){
								Builder.cfgStartNode = Builder.currentNode;
							}
							else{
								Builder.cfgStartNode.addChild(Builder.currentNode);
							    Builder.currentNode.addParent(Builder.cfgStartNode);
							}

							Builder.terminalNodes.add(Builder.currentNode);
							forNode = Builder.currentNode;
							ForStatementVisitor forVisitor = new ForStatementVisitor();
							forVisitor.shouldVisitStatements = true;
							iacsChildren[i].accept(forVisitor);
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							//处理continue节点
							if(Builder.continueNodes.size()>0){
								for(CFGNode continueNode : Builder.continueNodes){
									forNode.addParent(continueNode);
									continueNode.addChild(forNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						}else{
							lastStatementOffset = iacsChildren[i].getFileLocation().getNodeOffset();
							
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = iacsChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=0; k<terminalNodesNum; k++){
									Builder.terminalNodes.get(k).addChild(nextNode);
									nextNode.addParent(Builder.terminalNodes.get(k));
								}
								Builder.terminalNodes = new ArrayList<CFGNode>();
								
								Builder.currentNode = new CFGNode();
								Builder.currentNode.addParent(nextNode);
								nextNode.addChild(Builder.currentNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=0; k<terminalNodesNum; k++){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
								}
								Builder.terminalNodes = new ArrayList<CFGNode>();
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
							}
							
							ForStatementVisitor forVisitor = new ForStatementVisitor();
							forVisitor.shouldVisitStatements = true;
							iacsChildren[i].accept(forVisitor);
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							//处理continue节点
							if(Builder.continueNodes.size()>0){
								for(CFGNode continueNode : Builder.continueNodes){
									forNode.addParent(continueNode);
									continueNode.addChild(forNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						}
					}
					else if(iacsChildren[i] instanceof IASTWhileStatement){
						/****************************
						* 			While语句	    *
						****************************/
                        countI++;
                        endStatementIndex = i;
                        CFGNode whileNode;
						if(countI == 1){
							lastStatementOffset = iacsChildren[i].getFileLocation().getNodeOffset();
							if(i==0){
								Builder.cfgStartNode = Builder.currentNode;
							}
							else{
								Builder.cfgStartNode.addChild(Builder.currentNode);
							    Builder.currentNode.addParent(Builder.cfgStartNode);
							}							
							Builder.terminalNodes.add(Builder.currentNode);
							whileNode = Builder.currentNode;
							WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
							whileVisitor.shouldVisitStatements = true;
							iacsChildren[i].accept(whileVisitor);
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							//处理continue节点
							if(Builder.continueNodes.size()>0){
								for(CFGNode continueNode : Builder.continueNodes){
									whileNode.addParent(continueNode);
									continueNode.addChild(whileNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						}else{
							lastStatementOffset = iacsChildren[i].getFileLocation().getNodeOffset();
							
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = iacsChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=0; k<terminalNodesNum; k++){
									Builder.terminalNodes.get(k).addChild(nextNode);
									nextNode.addParent(Builder.terminalNodes.get(k));
								}
								Builder.terminalNodes = new ArrayList<CFGNode>();
								
								Builder.currentNode = new CFGNode();
								Builder.currentNode.addParent(nextNode);
								nextNode.addChild(Builder.currentNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=0; k<terminalNodesNum; k++){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
								}
								Builder.terminalNodes = new ArrayList<CFGNode>();
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
							}
							
							WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
							whileVisitor.shouldVisitStatements = true;
							iacsChildren[i].accept(whileVisitor);
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							//处理continue节点
							if(Builder.continueNodes.size()>0){
								for(CFGNode continueNode : Builder.continueNodes){
									whileNode.addParent(continueNode);
									continueNode.addChild(whileNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						}
					}
					else if(iacsChildren[i] instanceof IASTDoStatement){
						/****************************
						* 			Do-while语句	    *
						****************************/
                        countI++;
                        endStatementIndex = i;
						if(countI == 1){
							lastStatementOffset = iacsChildren[i].getFileLocation().getNodeOffset();
							if(i==0){
								Builder.cfgStartNode = Builder.currentNode;
							}
							else{
								Builder.cfgStartNode.addChild(Builder.currentNode);
							    Builder.currentNode.addParent(Builder.cfgStartNode);
							}							
							
							DoStatementVisitor doVisitor = new DoStatementVisitor();
							doVisitor.shouldVisitStatements = true;
							iacsChildren[i].accept(doVisitor);
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						}else{
							lastStatementOffset = iacsChildren[i].getFileLocation().getNodeOffset();
							
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = iacsChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=0; k<terminalNodesNum; k++){
									Builder.terminalNodes.get(k).addChild(nextNode);
									nextNode.addParent(Builder.terminalNodes.get(k));
								}
								Builder.terminalNodes = new ArrayList<CFGNode>();
								
								Builder.currentNode = new CFGNode();
								Builder.currentNode.addParent(nextNode);
								nextNode.addChild(Builder.currentNode);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=0; k<terminalNodesNum; k++){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
								}
								Builder.terminalNodes = new ArrayList<CFGNode>();
							}
							
							DoStatementVisitor doVisitor = new DoStatementVisitor();
							doVisitor.shouldVisitStatements = true;
							iacsChildren[i].accept(doVisitor);
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						}
					}
					else if(iacsChildren[i] instanceof IASTReturnStatement){
						/****************************
						* 		Return语句			*
						****************************/
						endStatementIndex = i;
						if(firstSequenceStatementIndex == (n-1)){
							Builder.currentNode = new CFGNode();
							int offsetTmp = iacsChildren[i].getFileLocation().getNodeOffset();
							Builder.currentNode.setOffset(offsetTmp);
							Builder.currentNode.setSign(4);
							Builder.nodeNumber++;
							Builder.currentNode.setNodeNumber(Builder.nodeNumber);
							Builder.currentNode.setIfChild(null);
							Builder.currentNode.setElseChild(null);
	
							//为当前顺序节点添加父节点
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastStatementOffset){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							return PROCESS_ABORT;
						}else{
							Builder.currentNode = new CFGNode();
							int offsetTmp = iacsChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							Builder.currentNode.setOffset(offsetTmp);
							Builder.currentNode.setSign(-1);
							Builder.nodeNumber++;
							Builder.currentNode.setNodeNumber(Builder.nodeNumber);
							//为当前顺序节点添加父节点
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastStatementOffset){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							CFGNode returnNode = new CFGNode();
							offsetTmp = iacsChildren[i].getFileLocation().getNodeOffset();
							returnNode.setOffset(offsetTmp);
							returnNode.setSign(4);
							Builder.nodeNumber++;
							returnNode.setNodeNumber(Builder.nodeNumber);
							
							Builder.currentNode.addChild(returnNode);
							returnNode.addParent(Builder.currentNode);
						}
					}
				}
				if(endStatementIndex > -1 && endStatementIndex < n-1){
					CFGNode nextNode = new CFGNode();
					int offsetTmp = iacsChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
					nextNode.setOffset(offsetTmp);
					nextNode.setSign(-1);
					Builder.nodeNumber++;
					nextNode.setNodeNumber(Builder.nodeNumber);
					
					//为当前顺序节点添加父节点
					int terminalNodesNum = Builder.terminalNodes.size();
					for(int k=(terminalNodesNum-1); k>-1; k--){
						if(Builder.terminalNodes.get(k).getOffset() >= lastStatementOffset){
							Builder.terminalNodes.get(k).addChild(nextNode);
							nextNode.addParent(Builder.terminalNodes.get(k));
							Builder.terminalNodes.remove(k);
						}
					}
				}
			}
		}
		
		return PROCESS_CONTINUE;
	}
}
