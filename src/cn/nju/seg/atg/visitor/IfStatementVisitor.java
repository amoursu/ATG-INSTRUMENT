package cn.nju.seg.atg.visitor;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import cn.nju.seg.atg.parse.Builder;

/**
 * 
 * @author ChengXin 
 * @author zy
 * 
 */
public class IfStatementVisitor extends ASTVisitor {

	public IfStatementVisitor() { }

	public int visit(IASTStatement node) {
		if (node instanceof IASTIfStatement) {
			IASTIfStatement iais = (IASTIfStatement) node;
			IASTBinaryExpression iabe = (IASTBinaryExpression) iais.getChildren()[0];
			int offsetTmp = iabe.getFileLocation().getNodeOffset();
			Builder.currentNode.setSign(3);
			Builder.currentNode.setBinaryExpression(iabe);
			Builder.currentNode.setOffset(offsetTmp);
			Builder.nodeNumber++;
			Builder.currentNode.setNodeNumber(Builder.nodeNumber);
			
			addNode(Builder.currentNode, iais);

			return PROCESS_ABORT;
		}

		return PROCESS_CONTINUE;
	}
	
	public void addNode(CFGNode parent, IASTIfStatement iais) {
		CFGNode ifNode = new CFGNode();
		CFGNode elseNode = new CFGNode();
		int lastIfStatementOffset = -1;
		int endStatementIndex = -1;	//记录代码块中最后一个statement的下标，通过对比推出该代码块中最后一条语句是否为statement
		if (iais.getChildren().length > 2) { // 有else语句
			 /********************************************************
			 * if-else语句的if分支和else分支都是IASTCompoundStatement *
			 ********************************************************/
			if ((iais.getChildren()[1] instanceof IASTCompoundStatement)
					&& (iais.getChildren()[2] instanceof IASTCompoundStatement)) {
				IASTCompoundStatement ifCompound = (IASTCompoundStatement) iais.getChildren()[1];
				IASTCompoundStatement elseCompound = (IASTCompoundStatement) iais.getChildren()[2];

				// 处理if分支
				IASTNode[] ifCompoundChildren = ifCompound.getChildren();
				int ifCompoundChildrenN = ifCompoundChildren.length;
				if(ifCompoundChildrenN>0){
					if(ifCompoundChildren[0] instanceof IASTIfStatement){ }            //if-else
					else if(ifCompoundChildren[0] instanceof IASTForStatement){ }      //for
					else if(ifCompoundChildren[0] instanceof IASTWhileStatement){ }    //while
					else if(ifCompoundChildren[0] instanceof IASTDoStatement){ }       //do-while
					else if(ifCompoundChildren[0] instanceof IASTReturnStatement){ }   //return
					else if(ifCompoundChildren[0] instanceof IASTBreakStatement){ }    //break
					else if(ifCompoundChildren[0] instanceof IASTContinueStatement){ } //continue
					else{
						int offsetTmp = ifCompound.getFileLocation().getNodeOffset();
						ifNode.setSign(0);
						ifNode.setOffset(offsetTmp);
						Builder.nodeNumber++;
						ifNode.setNodeNumber(Builder.nodeNumber);
						parent.setIfChild(ifNode);
						ifNode.addParent(parent);
					}
				}
				int countI = 0;
				int firstSequenceStatementIndex = -1;
				for (int i = 0; i < ifCompoundChildrenN; i++) {
					if(ifCompoundChildren[i] instanceof IASTReturnStatement) {
						/*******************************
						 *  默认return为当前模块的最后一句  *
						 *******************************/	
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "return语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(4);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
						    }else{
						    	 CFGNode returnNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 returnNode.setOffset(offsetTmp);
								 returnNode.setSign(4);
								 Builder.nodeNumber++;
								 returnNode.setNodeNumber(Builder.nodeNumber);
								 returnNode.addParent(ifNode);
								 ifNode.addChild(returnNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode returnNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(returnNode);
									    returnNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode returnNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(returnNode);
							    returnNode.addParent(nextNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTBreakStatement) {
						/*******************************
						 *  默认break为当前模块的最后一句  *
						 *******************************/	
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "break语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(7);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
							    Builder.breakNodes.add(ifNode);
						    }else{
						    	 CFGNode breakNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 breakNode.setOffset(offsetTmp);
								 breakNode.setSign(7);
								 Builder.nodeNumber++;
								 breakNode.setNodeNumber(Builder.nodeNumber);
								 breakNode.addParent(ifNode);
								 ifNode.addChild(breakNode);
								 Builder.breakNodes.add(breakNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode breakNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    Builder.breakNodes.add(breakNode);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(breakNode);
									    breakNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode breakNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(breakNode);
							    breakNode.addParent(nextNode);
							    Builder.breakNodes.add(breakNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTContinueStatement) {
						/*******************************
						 *  默认continue为当前模块的最后一句  *
						 *******************************/	
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "continue语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(8);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
							    Builder.continueNodes.add(ifNode);
						    }else{
						    	 CFGNode continueNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 continueNode.setOffset(offsetTmp);
								 continueNode.setSign(8);
								 Builder.nodeNumber++;
								 continueNode.setNodeNumber(Builder.nodeNumber);
								 continueNode.addParent(ifNode);
								 ifNode.addChild(continueNode);
								 Builder.continueNodes.add(continueNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode continueNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    continueNode.setOffset(offsetTmp);
							    continueNode.setSign(8);
							    Builder.nodeNumber++;
							    continueNode.setNodeNumber(Builder.nodeNumber);
							    Builder.continueNodes.add(continueNode);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(continueNode);
									    continueNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode continueNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    continueNode.setOffset(offsetTmp);
							    continueNode.setSign(8);
							    Builder.nodeNumber++;
							    continueNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(continueNode);
							    continueNode.addParent(nextNode);
							    Builder.continueNodes.add(continueNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTIfStatement) {
						countI++; // 标记该if分支内是否有if-else语句						
						endStatementIndex = i;

						if (countI == 1) { // 当前if分支内有且仅有一个if-else语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							IASTBinaryExpression iabe = (IASTBinaryExpression) ifCompoundChildren[i].getChildren()[0];
							int offsetTmp = iabe.getFileLocation().getNodeOffset();
							if(i==0){
								ifNode.setBinaryExpression(iabe);
								ifNode.setOffset(offsetTmp);
								ifNode.setSign(3);
								Builder.nodeNumber++;
								ifNode.setNodeNumber(Builder.nodeNumber);
								parent.setIfChild(ifNode);
								ifNode.addParent(parent);
	
								addNode(ifNode,(IASTIfStatement) ifCompoundChildren[i]);
							}else{
								CFGNode nextNode = new CFGNode();
								nextNode.setBinaryExpression(iabe);
								nextNode.setOffset(offsetTmp);
								nextNode.setSign(3);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								ifNode.addChild(nextNode);
								nextNode.addParent(ifNode);
								
								addNode(nextNode, (IASTIfStatement)ifCompoundChildren[i]);
							}
						} else { // 当前if分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression)ifCompoundChildren[i].getChildren()[0];
								offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								nextNode.addChild(nextIfNode);
								nextIfNode.addParent(nextNode);
								
								addNode(nextIfNode, (IASTIfStatement)ifCompoundChildren[i]);
							}else{
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression) ifCompoundChildren[i].getChildren()[0];
								int offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextIfNode);
										nextIfNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								addNode(nextIfNode, (IASTIfStatement)ifCompoundChildren[i]);
							}
						}
						firstSequenceStatementIndex = i+1;
					}else if(ifCompoundChildren[i] instanceof IASTForStatement){
						countI++; // 标记该if分支内是否有if-else语句
						
						endStatementIndex = i;
                        CFGNode forNode;
						if (countI == 1) { // 当前if分支内有且仅有一个for语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}
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
						} else { // 当前if分支内有2个或2个以上的if-else语句或者for语句
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}
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
					}else if(ifCompoundChildren[i] instanceof IASTWhileStatement){
						countI++;						
						endStatementIndex = i;
                        CFGNode whileNode;
						if (countI == 1) { // 当前if分支内有且仅有一个while语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}
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
						} else {
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}
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
					}else if(ifCompoundChildren[i] instanceof IASTDoStatement){
						countI++; 						
						endStatementIndex = i;

						if (countI == 1) { // 当前if分支内有且仅有一个do-while语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						} else { 
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}
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
				}
				if (countI == 0) { // countI==0表明：当前if分支块中没有if-else语句
					Builder.terminalNodes.add(ifNode);
				}
				//当前块中最后的语句是顺序语句
				if(endStatementIndex > -1 && endStatementIndex < ifCompoundChildrenN-1){
					CFGNode nextNode = new CFGNode();
					int offsetTmp = ifCompoundChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
					nextNode.setOffset(offsetTmp);
					nextNode.setSign(-1);
					Builder.nodeNumber++;
					nextNode.setNodeNumber(Builder.nodeNumber);
					
					// 为当前顺序节点添加父节点
					int terminalNodesNum = Builder.terminalNodes.size();
					for (int k = (terminalNodesNum - 1); k > -1; k--) {
						if (Builder.terminalNodes.get(k).getOffset() < lastIfStatementOffset) {
							break;
						} else {
							Builder.terminalNodes.get(k).addChild(nextNode);
							nextNode.addParent(Builder.terminalNodes.get(k));
							Builder.terminalNodes.remove(k);
						}
					}
					Builder.terminalNodes.add(nextNode);
				}
				endStatementIndex = -1;	//重新初始化endStatementIndex=-1
				firstSequenceStatementIndex = -1; //重新初始化firstSequenceStatementIndex=-1

				// 处理else分支
				IASTNode[] elseCompoundChildren = elseCompound.getChildren();
				int elseCompoundChildrenN = elseCompoundChildren.length;
				if(elseCompoundChildrenN > 0){
					if(elseCompoundChildren[0] instanceof IASTIfStatement){ }            //if-else
					else if(elseCompoundChildren[0] instanceof IASTForStatement){ }      //for
					else if(elseCompoundChildren[0] instanceof IASTWhileStatement){ }    //while
					else if(elseCompoundChildren[0] instanceof IASTDoStatement){ }       //do-while
					else if(elseCompoundChildren[0] instanceof IASTReturnStatement){ }   //return
					else if(elseCompoundChildren[0] instanceof IASTBreakStatement){ }    //break
					else if(elseCompoundChildren[0] instanceof IASTContinueStatement){ } //continue
					else{
						int offsetTmp = elseCompound.getFileLocation().getNodeOffset();
						elseNode.setSign(1);
						elseNode.setOffset(offsetTmp);
						Builder.nodeNumber++;
						elseNode.setNodeNumber(Builder.nodeNumber);
						parent.setElseChild(elseNode);
						elseNode.addParent(parent);
					}
				}
				int countJ = 0;
				for (int j = 0; j < elseCompoundChildrenN; j++) {
					if(elseCompoundChildren[j] instanceof IASTReturnStatement) {
						countJ++; 					
						endStatementIndex = j;
						if(j!=elseCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "return语句位置存在错误！");
						}
						if(countJ==1){
							if(j==0){
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    elseNode.setOffset(offsetTmp);
							    elseNode.setSign(4);
							    Builder.nodeNumber++;
							    elseNode.setNodeNumber(Builder.nodeNumber);
							    elseNode.addParent(parent);
							    parent.setElseChild(elseNode);
						    }else{
						    	 CFGNode returnNode = new CFGNode();
								 int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
								 returnNode.setOffset(offsetTmp);
								 returnNode.setSign(4);
								 Builder.nodeNumber++;
								 returnNode.setNodeNumber(Builder.nodeNumber);
								 returnNode.addParent(elseNode);
								 elseNode.addChild(returnNode);
							}
						}else{
							if(firstSequenceStatementIndex == (elseCompoundChildrenN-1)){
							    CFGNode returnNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(returnNode);
									    returnNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode returnNode = new CFGNode();
							    offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(returnNode);
							    returnNode.addParent(nextNode);
						    }
						}
					}else if (elseCompoundChildren[j] instanceof IASTBreakStatement) { 
						countJ++; 					
						endStatementIndex = j;
						if(j!=elseCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "break语句位置存在错误！");
						}
						if(countJ==1){
							if(j==0){
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    elseNode.setOffset(offsetTmp);
							    elseNode.setSign(7);
							    Builder.nodeNumber++;
							    elseNode.setNodeNumber(Builder.nodeNumber);
							    elseNode.addParent(parent);
							    parent.setElseChild(elseNode);
							    Builder.breakNodes.add(elseNode);
						    }else{
						    	 CFGNode breakNode = new CFGNode();
								 int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
								 breakNode.setOffset(offsetTmp);
								 breakNode.setSign(7);
								 Builder.nodeNumber++;
								 breakNode.setNodeNumber(Builder.nodeNumber);
								 breakNode.addParent(elseNode);
								 elseNode.addChild(breakNode);
								 Builder.breakNodes.add(breakNode);
							}
						}else{
							if(firstSequenceStatementIndex == (elseCompoundChildrenN-1)){
							    CFGNode breakNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    Builder.breakNodes.add(breakNode);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(breakNode);
									    breakNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode breakNode = new CFGNode();
							    offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(breakNode);
							    breakNode.addParent(nextNode);
							    Builder.breakNodes.add(breakNode);
						    }
						}
					}else if (elseCompoundChildren[j] instanceof IASTContinueStatement) {
						countJ++; 					
						endStatementIndex = j;
						if(j!=elseCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "continue语句位置存在错误！");
						}
						if(countJ==1){
							if(j==0){
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    elseNode.setOffset(offsetTmp);
							    elseNode.setSign(8);
							    Builder.nodeNumber++;
							    elseNode.setNodeNumber(Builder.nodeNumber);
							    elseNode.addParent(parent);
							    parent.setElseChild(elseNode);
							    Builder.continueNodes.add(elseNode);
						    }else{
						    	 CFGNode continueNode = new CFGNode();
								 int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
								 continueNode.setOffset(offsetTmp);
								 continueNode.setSign(8);
								 Builder.nodeNumber++;
								 continueNode.setNodeNumber(Builder.nodeNumber);
								 continueNode.addParent(elseNode);
								 elseNode.addChild(continueNode);
								 Builder.continueNodes.add(continueNode);
							}
						}else{
							if(firstSequenceStatementIndex == (elseCompoundChildrenN-1)){
							    CFGNode continueNodes = new CFGNode();
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    continueNodes.setOffset(offsetTmp);
							    continueNodes.setSign(8);
							    Builder.nodeNumber++;
							    continueNodes.setNodeNumber(Builder.nodeNumber);
							    Builder.continueNodes.add(continueNodes);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(continueNodes);
									    continueNodes.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode continueNodes = new CFGNode();
							    offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    continueNodes.setOffset(offsetTmp);
							    continueNodes.setSign(8);
							    Builder.nodeNumber++;
							    continueNodes.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(continueNodes);
							    continueNodes.addParent(nextNode);
							    Builder.continueNodes.add(continueNodes);
						    }
						}
					}else if (elseCompoundChildren[j] instanceof IASTIfStatement) {
						countJ++; // 标记该else分支内是否有if-else语句
						
						endStatementIndex = j;

						if (countJ == 1) { // 当前else分支内有且仅有一个if-else语句
							lastIfStatementOffset = elseCompoundChildren[j].getFileLocation().getNodeOffset();

							IASTBinaryExpression iabe = (IASTBinaryExpression) elseCompoundChildren[j].getChildren()[0];
							int offsetTmp = iabe.getFileLocation().getNodeOffset();
							if(j==0){
								elseNode.setBinaryExpression(iabe);
								elseNode.setOffset(offsetTmp);
								elseNode.setSign(3);
								Builder.nodeNumber++;
								elseNode.setNodeNumber(Builder.nodeNumber);
								parent.setElseChild(elseNode);
								elseNode.addParent(parent);
	
								addNode(elseNode,(IASTIfStatement) elseCompoundChildren[j]);
							}else{
								CFGNode nextNode = new CFGNode();
								nextNode.setBinaryExpression(iabe);
								nextNode.setOffset(offsetTmp);
								nextNode.setSign(3);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								elseNode.addChild(nextNode);
								nextNode.addParent(elseNode);
								
								addNode(nextNode, (IASTIfStatement)elseCompoundChildren[j]);
							}
							
							firstSequenceStatementIndex = j+1;
						} else { // 当前else分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != j){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression)elseCompoundChildren[j].getChildren()[0];
								offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								nextNode.addChild(nextIfNode);
								nextIfNode.addParent(nextNode);
								
								addNode(nextIfNode, (IASTIfStatement)elseCompoundChildren[j]);
							}else{
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression) elseCompoundChildren[j].getChildren()[0];
								int offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextIfNode);
										nextIfNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								addNode(nextIfNode, (IASTIfStatement)elseCompoundChildren[j]);
							}
							firstSequenceStatementIndex = j+1;
						}
					}else if(elseCompoundChildren[j] instanceof IASTForStatement){
						countJ++; // 标记该else分支内是否有if-else语句
						
						endStatementIndex = j;
                        CFGNode forNode;
						if (countJ == 1) { // 当前else分支内有且仅有一个if-else语句
							lastIfStatementOffset = elseCompoundChildren[j].getFileLocation().getNodeOffset();

							if(j==0){
								Builder.currentNode = new CFGNode();
								parent.setElseChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								elseNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(elseNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(forVisitor);
							}
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
							firstSequenceStatementIndex = j+1;
						} else { // 当前else分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != j){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(forVisitor);
							}
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
							firstSequenceStatementIndex = j+1;
						}
					}else if(elseCompoundChildren[j] instanceof IASTWhileStatement){
						countJ++; 					
						endStatementIndex = j;
						CFGNode whileNode;
						if (countJ == 1) {
							lastIfStatementOffset = elseCompoundChildren[j].getFileLocation().getNodeOffset();

							if(j==0){
								Builder.currentNode = new CFGNode();
								parent.setElseChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								elseNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(elseNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(whileVisitor);
							}
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
							firstSequenceStatementIndex = j+1;
						} else { // 当前else分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != j){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(whileVisitor);
							}
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
							firstSequenceStatementIndex = j+1;
						}
					}else if(elseCompoundChildren[j] instanceof IASTDoStatement){
						countJ++; 					
						endStatementIndex = j;

						if (countJ == 1) {
							lastIfStatementOffset = elseCompoundChildren[j].getFileLocation().getNodeOffset();

							if(j==0){
								Builder.currentNode = new CFGNode();
								parent.setElseChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								elseNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(elseNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(doVisitor);
							}
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = j+1;
						} else { // 当前else分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != j){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(doVisitor);
							}
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = j+1;
						}
					}
				}
				if (countJ == 0) { // countJ==0表明：当前else分支块中没有if-else语句
					Builder.terminalNodes.add(elseNode);
				}
				//当前块中的最后语句是顺序语句
				if(endStatementIndex > -1 && endStatementIndex < elseCompoundChildrenN-1){
					CFGNode nextNode = new CFGNode();
					int offsetTmp = elseCompoundChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
					nextNode.setOffset(offsetTmp);
					nextNode.setSign(-1);
					Builder.nodeNumber++;
					nextNode.setNodeNumber(Builder.nodeNumber);
					
					// 为当前顺序节点添加父节点
					int terminalNodesNum = Builder.terminalNodes.size();
					for (int k = (terminalNodesNum - 1); k > -1; k--) {
						if (Builder.terminalNodes.get(k).getOffset() < lastIfStatementOffset) {
							break;
						} else {
							Builder.terminalNodes.get(k).addChild(nextNode);
							nextNode.addParent(Builder.terminalNodes.get(k));
							Builder.terminalNodes.remove(k);
						}
					}
					Builder.terminalNodes.add(nextNode);
				}
				endStatementIndex = -1;	//重新初始化endStatementIndex=-1
				firstSequenceStatementIndex = -1; //重新初始化firstSequenceStatementIndex=-1
			} else if (iais.getChildren()[1] instanceof IASTCompoundStatement) {
				 /*******************************************************
				 * 当且仅当if分支是IASTCompoundStatement				    *
				 *******************************************************/
				IASTCompoundStatement ifCompound = (IASTCompoundStatement) iais.getChildren()[1];

				// 处理if分支
				IASTNode[] ifCompoundChildren = ifCompound.getChildren();
				int ifCompoundChildrenN = ifCompoundChildren.length;
				if(ifCompoundChildrenN>0){
					if(ifCompoundChildren[0] instanceof IASTIfStatement){ }            //if-else
					else if(ifCompoundChildren[0] instanceof IASTForStatement){ }      //for
					else if(ifCompoundChildren[0] instanceof IASTWhileStatement){ }    //while
					else if(ifCompoundChildren[0] instanceof IASTDoStatement){ }       //do-while
					else if(ifCompoundChildren[0] instanceof IASTReturnStatement){ }   //return
					else if(ifCompoundChildren[0] instanceof IASTBreakStatement){ }    //break
					else if(ifCompoundChildren[0] instanceof IASTContinueStatement){ } //continue
					else{
						int offsetTmp = ifCompound.getFileLocation().getNodeOffset();
						ifNode.setSign(0);
						ifNode.setOffset(offsetTmp);
						Builder.nodeNumber++;
						ifNode.setNodeNumber(Builder.nodeNumber);
						parent.setIfChild(ifNode);
						ifNode.addParent(parent);
					}
				}
				int countI = 0;
				int firstSequenceStatementIndex = -1;
				for (int i = 0; i < ifCompoundChildrenN; i++) {
					if(ifCompoundChildren[i] instanceof IASTReturnStatement) {
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "return语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(4);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
						    }else{
						    	 CFGNode returnNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 returnNode.setOffset(offsetTmp);
								 returnNode.setSign(4);
								 Builder.nodeNumber++;
								 returnNode.setNodeNumber(Builder.nodeNumber);
								 returnNode.addParent(ifNode);
								 ifNode.addChild(returnNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode returnNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(returnNode);
									    returnNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode returnNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(returnNode);
							    returnNode.addParent(nextNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTBreakStatement) {
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "break语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(7);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
							    Builder.breakNodes.add(ifNode);
						    }else{
						    	 CFGNode breakNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 breakNode.setOffset(offsetTmp);
								 breakNode.setSign(7);
								 Builder.nodeNumber++;
								 breakNode.setNodeNumber(Builder.nodeNumber);
								 breakNode.addParent(ifNode);
								 ifNode.addChild(breakNode);
								 Builder.breakNodes.add(breakNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode breakNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    Builder.breakNodes.add(breakNode);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(breakNode);
									    breakNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode breakNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(breakNode);
							    breakNode.addParent(nextNode);
							    Builder.breakNodes.add(breakNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTContinueStatement) {
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "continue语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(8);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
							    Builder.continueNodes.add(ifNode);
						    }else{
						    	 CFGNode continueNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 continueNode.setOffset(offsetTmp);
								 continueNode.setSign(8);
								 Builder.nodeNumber++;
								 continueNode.setNodeNumber(Builder.nodeNumber);
								 continueNode.addParent(ifNode);
								 ifNode.addChild(continueNode);
								 Builder.continueNodes.add(continueNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode continueNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    continueNode.setOffset(offsetTmp);
							    continueNode.setSign(8);
							    Builder.nodeNumber++;
							    continueNode.setNodeNumber(Builder.nodeNumber);
							    Builder.continueNodes.add(continueNode);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(continueNode);
									    continueNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode continueNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    continueNode.setOffset(offsetTmp);
							    continueNode.setSign(8);
							    Builder.nodeNumber++;
							    continueNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(continueNode);
							    continueNode.addParent(nextNode);
							    Builder.continueNodes.add(continueNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTIfStatement) {
						countI++; // 标记该if分支内是否有if-else语句						
						endStatementIndex = i;

						if (countI == 1) { // 当前if分支内有且仅有一个if-else语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							IASTBinaryExpression iabe = (IASTBinaryExpression) ifCompoundChildren[i].getChildren()[0];
							int offsetTmp = iabe.getFileLocation().getNodeOffset();
							if(i==0){
								ifNode.setBinaryExpression(iabe);
								ifNode.setOffset(offsetTmp);
								ifNode.setSign(3);
								Builder.nodeNumber++;
								ifNode.setNodeNumber(Builder.nodeNumber);
								parent.setIfChild(ifNode);
								ifNode.addParent(parent);
	
								addNode(ifNode,(IASTIfStatement) ifCompoundChildren[i]);
							}else{
								CFGNode nextNode = new CFGNode();
								nextNode.setBinaryExpression(iabe);
								nextNode.setOffset(offsetTmp);
								nextNode.setSign(3);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								ifNode.addChild(nextNode);
								nextNode.addParent(ifNode);
								
								addNode(nextNode, (IASTIfStatement)ifCompoundChildren[i]);
							}
						} else { // 当前if分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression)ifCompoundChildren[i].getChildren()[0];
								offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								nextNode.addChild(nextIfNode);
								nextIfNode.addParent(nextNode);
								
								addNode(nextIfNode, (IASTIfStatement)ifCompoundChildren[i]);
							}else{
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression) ifCompoundChildren[i].getChildren()[0];
								int offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextIfNode);
										nextIfNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								addNode(nextIfNode, (IASTIfStatement)ifCompoundChildren[i]);
							}
						}
						firstSequenceStatementIndex = i+1;
					}else if(ifCompoundChildren[i] instanceof IASTForStatement){
						countI++; // 标记该if分支内是否有if-else语句
						
						endStatementIndex = i;
                        CFGNode forNode;
						if (countI == 1) { // 当前if分支内有且仅有一个for语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}
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
						} else { // 当前if分支内有2个或2个以上的if-else语句或者for语句
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}
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
					}else if(ifCompoundChildren[i] instanceof IASTWhileStatement){
						countI++;						
						endStatementIndex = i;
                        CFGNode whileNode;
						if (countI == 1) { // 当前if分支内有且仅有一个while语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}
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
						} else {
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}
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
					}else if(ifCompoundChildren[i] instanceof IASTDoStatement){
						countI++; 						
						endStatementIndex = i;

						if (countI == 1) { // 当前if分支内有且仅有一个do-while语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						} else { 
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}
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
				}
				if (countI == 0) {
					Builder.terminalNodes.add(ifNode);
				}
				//当前块中最后的语句是顺序语句
				if(endStatementIndex > -1 && endStatementIndex < ifCompoundChildrenN-1){
					CFGNode nextNode = new CFGNode();
					int offsetTmp = ifCompoundChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
					nextNode.setOffset(offsetTmp);
					nextNode.setSign(-1);
					Builder.nodeNumber++;
					nextNode.setNodeNumber(Builder.nodeNumber);
					
					// 为当前顺序节点添加父节点
					int terminalNodesNum = Builder.terminalNodes.size();
					for (int k = (terminalNodesNum - 1); k > -1; k--) {
						if (Builder.terminalNodes.get(k).getOffset() < lastIfStatementOffset) {
							break;
						} else {
							Builder.terminalNodes.get(k).addChild(nextNode);
							nextNode.addParent(Builder.terminalNodes.get(k));
							Builder.terminalNodes.remove(k);
						}
					}
					Builder.terminalNodes.add(nextNode);
				}
				endStatementIndex = -1;	//重新初始化endStatementIndex=-1
				firstSequenceStatementIndex = -1; //重新初始化firstSequenceStatementIndex=-1

				// else分支
				if(iais.getChildren()[2] instanceof IASTIfStatement){
					IASTBinaryExpression iabe = (IASTBinaryExpression)iais.getChildren()[2].getChildren()[0];
					int offsetTmp = iabe.getFileLocation().getNodeOffset();
					elseNode.setBinaryExpression(iabe);
					elseNode.setOffset(offsetTmp);
					elseNode.setSign(3);
					Builder.nodeNumber++;
					elseNode.setNodeNumber(Builder.nodeNumber);
					parent.setElseChild(elseNode);
					elseNode.addParent(parent);	
					
					addNode(elseNode, (IASTIfStatement)iais.getChildren()[2]);
				}else if(iais.getChildren()[2] instanceof IASTForStatement){
					Builder.currentNode = new CFGNode();
					parent.setElseChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);					
					Builder.terminalNodes.add(Builder.currentNode);		
					CFGNode forNode = Builder.currentNode;
					ForStatementVisitor forVisitor = new ForStatementVisitor();
					forVisitor.shouldVisitStatements = true;
					iais.getChildren()[2].accept(forVisitor);
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
				}else if(iais.getChildren()[2] instanceof IASTWhileStatement){
					Builder.currentNode = new CFGNode();
					parent.setElseChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);					
					Builder.terminalNodes.add(Builder.currentNode);	
					CFGNode whileNode = Builder.currentNode;
					WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
					whileVisitor.shouldVisitStatements = true;
					iais.getChildren()[2].accept(whileVisitor);
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
				}else if(iais.getChildren()[2] instanceof IASTDoStatement){
					Builder.currentNode = new CFGNode();
					parent.setElseChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);		
					
					DoStatementVisitor doVisitor = new DoStatementVisitor();
					doVisitor.shouldVisitStatements = true;
					iais.getChildren()[2].accept(doVisitor);
					//处理break节点
					if(Builder.breakNodes.size()>0){
						for(CFGNode breakNode : Builder.breakNodes){
							Builder.terminalNodes.add(breakNode);
						}
					}
					Builder.breakNodes = new ArrayList<CFGNode>();
				}else if(iais.getChildren()[2] instanceof IASTReturnStatement){
					 int offsetTmp = iais.getChildren()[2].getFileLocation().getNodeOffset();
					 elseNode.setOffset(offsetTmp);
					 elseNode.setSign(4);
					 Builder.nodeNumber++;
					 elseNode.setNodeNumber(Builder.nodeNumber);
					 elseNode.addParent(parent);
					 parent.setElseChild(elseNode);
				}else if(iais.getChildren()[2] instanceof IASTBreakStatement){
					int offsetTmp = iais.getChildren()[2].getFileLocation().getNodeOffset();
					 elseNode.setOffset(offsetTmp);
					 elseNode.setSign(7);
					 Builder.nodeNumber++;
					 elseNode.setNodeNumber(Builder.nodeNumber);
					 elseNode.addParent(parent);
					 parent.setElseChild(elseNode);
					 Builder.breakNodes.add(elseNode);
				}else if(iais.getChildren()[2] instanceof IASTContinueStatement){
					int offsetTmp = iais.getChildren()[2].getFileLocation().getNodeOffset();
					 elseNode.setOffset(offsetTmp);
					 elseNode.setSign(8);
					 Builder.nodeNumber++;
					 elseNode.setNodeNumber(Builder.nodeNumber);
					 elseNode.addParent(parent);
					 parent.setElseChild(elseNode);
					 Builder.continueNodes.add(elseNode);
				}else{
					int elseOffset = iais.getChildren()[2].getFileLocation().getNodeOffset();
					elseNode.setOffset(elseOffset);
					elseNode.setSign(1);
					Builder.nodeNumber++;
					elseNode.setNodeNumber(Builder.nodeNumber);
					parent.setElseChild(elseNode);
					elseNode.addParent(parent);
					Builder.terminalNodes.add(elseNode);
				}
			}else if(iais.getChildren()[2] instanceof IASTCompoundStatement){
				 /*******************************************************
				 * 当且仅当else分支是IASTCompoundStatement			    *
				 *******************************************************/
				IASTCompoundStatement elseCompound = (IASTCompoundStatement)iais.getChildren()[2];
				
				//if 分支
				if(iais.getChildren()[1] instanceof IASTIfStatement){
					IASTBinaryExpression iabe = (IASTBinaryExpression)iais.getChildren()[1].getChildren()[0];
					int offsetTmp = iabe.getFileLocation().getNodeOffset();
					ifNode.setBinaryExpression(iabe);
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(3);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					parent.setIfChild(ifNode);
					ifNode.addParent(parent);
					
					addNode(ifNode, (IASTIfStatement)iais.getChildren()[1]);
				}else if(iais.getChildren()[1] instanceof IASTForStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					Builder.terminalNodes.add(Builder.currentNode);
					CFGNode forNode = Builder.currentNode;
					ForStatementVisitor forVisitor = new ForStatementVisitor();
					forVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(forVisitor);
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
				}else if(iais.getChildren()[1] instanceof IASTWhileStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					Builder.terminalNodes.add(Builder.currentNode);
					CFGNode whileNode = Builder.currentNode;
					WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
					whileVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(whileVisitor);
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
				}else if(iais.getChildren()[1] instanceof IASTDoStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					
					DoStatementVisitor doVisitor = new DoStatementVisitor();
					doVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(doVisitor);
					//处理break节点
					if(Builder.breakNodes.size()>0){
						for(CFGNode breakNode : Builder.breakNodes){
							Builder.terminalNodes.add(breakNode);
						}
					}
					Builder.breakNodes = new ArrayList<CFGNode>();
				}else if(iais.getChildren()[1] instanceof IASTReturnStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(4);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
				}else if(iais.getChildren()[1] instanceof IASTBreakStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(7);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
					Builder.breakNodes.add(ifNode);
				}else if(iais.getChildren()[1] instanceof IASTContinueStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(8);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
					Builder.continueNodes.add(ifNode);
				}else{
					int ifOffset = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(ifOffset);
					ifNode.setSign(0);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					parent.setIfChild(ifNode);
					ifNode.addParent(parent);				
					Builder.terminalNodes.add(ifNode);
				}
				
				//else分支
				IASTNode[] elseCompoundChildren = elseCompound.getChildren();
				int elseCompoundChildrenN = elseCompoundChildren.length;
				if(elseCompoundChildrenN > 0){
					if(elseCompoundChildren[0] instanceof IASTIfStatement){ }            //if-else
					else if(elseCompoundChildren[0] instanceof IASTForStatement){ }      //for
					else if(elseCompoundChildren[0] instanceof IASTWhileStatement){ }    //while
					else if(elseCompoundChildren[0] instanceof IASTDoStatement){ }       //do-while
					else if(elseCompoundChildren[0] instanceof IASTReturnStatement){ }   //return
					else if(elseCompoundChildren[0] instanceof IASTBreakStatement){ }    //break
					else if(elseCompoundChildren[0] instanceof IASTContinueStatement){ } //continue
					else{
						int offsetTmp = elseCompound.getFileLocation().getNodeOffset();
						elseNode.setSign(1);
						elseNode.setOffset(offsetTmp);
						Builder.nodeNumber++;
						elseNode.setNodeNumber(Builder.nodeNumber);
						parent.setElseChild(elseNode);
						elseNode.addParent(parent);
					}
				}
				int countJ = 0;
				int firstSequenceStatementIndex = -1;
				for (int j = 0; j < elseCompoundChildrenN; j++) {
					if(elseCompoundChildren[j] instanceof IASTReturnStatement) {
						countJ++; 					
						endStatementIndex = j;
						if(j!=elseCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "return语句位置存在错误！");
						}
						if(countJ==1){
							if(j==0){
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    elseNode.setOffset(offsetTmp);
							    elseNode.setSign(4);
							    Builder.nodeNumber++;
							    elseNode.setNodeNumber(Builder.nodeNumber);
							    elseNode.addParent(parent);
							    parent.setElseChild(elseNode);
						    }else{
						    	 CFGNode returnNode = new CFGNode();
								 int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
								 returnNode.setOffset(offsetTmp);
								 returnNode.setSign(4);
								 Builder.nodeNumber++;
								 returnNode.setNodeNumber(Builder.nodeNumber);
								 returnNode.addParent(elseNode);
								 elseNode.addChild(returnNode);
							}
						}else{
							if(firstSequenceStatementIndex == (elseCompoundChildrenN-1)){
							    CFGNode returnNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(returnNode);
									    returnNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode returnNode = new CFGNode();
							    offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(returnNode);
							    returnNode.addParent(nextNode);
						    }
						}
					}else if (elseCompoundChildren[j] instanceof IASTBreakStatement) { 
						countJ++; 					
						endStatementIndex = j;
						if(j!=elseCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "break语句位置存在错误！");
						}
						if(countJ==1){
							if(j==0){
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    elseNode.setOffset(offsetTmp);
							    elseNode.setSign(7);
							    Builder.nodeNumber++;
							    elseNode.setNodeNumber(Builder.nodeNumber);
							    elseNode.addParent(parent);
							    parent.setElseChild(elseNode);
							    Builder.breakNodes.add(elseNode);
						    }else{
						    	 CFGNode breakNode = new CFGNode();
								 int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
								 breakNode.setOffset(offsetTmp);
								 breakNode.setSign(7);
								 Builder.nodeNumber++;
								 breakNode.setNodeNumber(Builder.nodeNumber);
								 breakNode.addParent(elseNode);
								 elseNode.addChild(breakNode);
								 Builder.breakNodes.add(breakNode);
							}
						}else{
							if(firstSequenceStatementIndex == (elseCompoundChildrenN-1)){
							    CFGNode breakNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    Builder.breakNodes.add(breakNode);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(breakNode);
									    breakNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode breakNode = new CFGNode();
							    offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(breakNode);
							    breakNode.addParent(nextNode);
							    Builder.breakNodes.add(breakNode);
						    }
						}
					}else if (elseCompoundChildren[j] instanceof IASTContinueStatement) {
						countJ++; 					
						endStatementIndex = j;
						if(j!=elseCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "continue语句位置存在错误！");
						}
						if(countJ==1){
							if(j==0){
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    elseNode.setOffset(offsetTmp);
							    elseNode.setSign(8);
							    Builder.nodeNumber++;
							    elseNode.setNodeNumber(Builder.nodeNumber);
							    elseNode.addParent(parent);
							    parent.setElseChild(elseNode);
							    Builder.continueNodes.add(elseNode);
						    }else{
						    	 CFGNode continueNode = new CFGNode();
								 int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
								 continueNode.setOffset(offsetTmp);
								 continueNode.setSign(8);
								 Builder.nodeNumber++;
								 continueNode.setNodeNumber(Builder.nodeNumber);
								 continueNode.addParent(elseNode);
								 elseNode.addChild(continueNode);
								 Builder.continueNodes.add(continueNode);
							}
						}else{
							if(firstSequenceStatementIndex == (elseCompoundChildrenN-1)){
							    CFGNode continueNodes = new CFGNode();
							    int offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    continueNodes.setOffset(offsetTmp);
							    continueNodes.setSign(8);
							    Builder.nodeNumber++;
							    continueNodes.setNodeNumber(Builder.nodeNumber);
							    Builder.continueNodes.add(continueNodes);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(continueNodes);
									    continueNodes.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode continueNodes = new CFGNode();
							    offsetTmp = elseCompoundChildren[j].getFileLocation().getNodeOffset();
							    continueNodes.setOffset(offsetTmp);
							    continueNodes.setSign(8);
							    Builder.nodeNumber++;
							    continueNodes.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(continueNodes);
							    continueNodes.addParent(nextNode);
							    Builder.continueNodes.add(continueNodes);
						    }
						}
					}else if (elseCompoundChildren[j] instanceof IASTIfStatement) {
						countJ++; // 标记该else分支内是否有if-else语句
						
						endStatementIndex = j;

						if (countJ == 1) { // 当前else分支内有且仅有一个if-else语句
							lastIfStatementOffset = elseCompoundChildren[j].getFileLocation().getNodeOffset();

							IASTBinaryExpression iabe = (IASTBinaryExpression) elseCompoundChildren[j].getChildren()[0];
							int offsetTmp = iabe.getFileLocation().getNodeOffset();
							if(j==0){
								elseNode.setBinaryExpression(iabe);
								elseNode.setOffset(offsetTmp);
								elseNode.setSign(3);
								Builder.nodeNumber++;
								elseNode.setNodeNumber(Builder.nodeNumber);
								parent.setElseChild(elseNode);
								elseNode.addParent(parent);
	
								addNode(elseNode,(IASTIfStatement) elseCompoundChildren[j]);
							}else{
								CFGNode nextNode = new CFGNode();
								nextNode.setBinaryExpression(iabe);
								nextNode.setOffset(offsetTmp);
								nextNode.setSign(3);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								elseNode.addChild(nextNode);
								nextNode.addParent(elseNode);
								
								addNode(nextNode, (IASTIfStatement)elseCompoundChildren[j]);
							}
							
							firstSequenceStatementIndex = j+1;
						} else { // 当前else分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != j){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression)elseCompoundChildren[j].getChildren()[0];
								offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								nextNode.addChild(nextIfNode);
								nextIfNode.addParent(nextNode);
								
								addNode(nextIfNode, (IASTIfStatement)elseCompoundChildren[j]);
							}else{
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression) elseCompoundChildren[j].getChildren()[0];
								int offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextIfNode);
										nextIfNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								addNode(nextIfNode, (IASTIfStatement)elseCompoundChildren[j]);
							}
							firstSequenceStatementIndex = j+1;
						}
					}else if(elseCompoundChildren[j] instanceof IASTForStatement){
						countJ++; // 标记该else分支内是否有if-else语句
						
						endStatementIndex = j;
                        CFGNode forNode;
						if (countJ == 1) { // 当前else分支内有且仅有一个if-else语句
							lastIfStatementOffset = elseCompoundChildren[j].getFileLocation().getNodeOffset();

							if(j==0){
								Builder.currentNode = new CFGNode();
								parent.setElseChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								elseNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(elseNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(forVisitor);
							}
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
							firstSequenceStatementIndex = j+1;
						} else { // 当前else分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != j){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(forVisitor);
							}
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
							firstSequenceStatementIndex = j+1;
						}
					}else if(elseCompoundChildren[j] instanceof IASTWhileStatement){
						countJ++; 					
						endStatementIndex = j;
						CFGNode whileNode;
						if (countJ == 1) {
							lastIfStatementOffset = elseCompoundChildren[j].getFileLocation().getNodeOffset();

							if(j==0){
								Builder.currentNode = new CFGNode();
								parent.setElseChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								elseNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(elseNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(whileVisitor);
							}
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
							firstSequenceStatementIndex = j+1;
						} else { // 当前else分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != j){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(whileVisitor);
							}
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
							firstSequenceStatementIndex = j+1;
						}
					}else if(elseCompoundChildren[j] instanceof IASTDoStatement){
						countJ++; 					
						endStatementIndex = j;

						if (countJ == 1) {
							lastIfStatementOffset = elseCompoundChildren[j].getFileLocation().getNodeOffset();

							if(j==0){
								Builder.currentNode = new CFGNode();
								parent.setElseChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								elseNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(elseNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(doVisitor);
							}
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = j+1;
						} else { // 当前else分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != j){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = elseCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								elseCompoundChildren[j].accept(doVisitor);
							}
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = j+1;
						}
					}
				}
				if (countJ == 0) { // countJ==0表明：当前else分支块中没有if-else语句
					Builder.terminalNodes.add(elseNode);
				}
				//当前块中的最后语句是顺序语句
				if(endStatementIndex > -1 && endStatementIndex < elseCompoundChildrenN-1){
					CFGNode nextNode = new CFGNode();
					int offsetTmp = elseCompoundChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
					nextNode.setOffset(offsetTmp);
					nextNode.setSign(-1);
					Builder.nodeNumber++;
					nextNode.setNodeNumber(Builder.nodeNumber);
					
					// 为当前顺序节点添加父节点
					int terminalNodesNum = Builder.terminalNodes.size();
					for (int k = (terminalNodesNum - 1); k > -1; k--) {
						if (Builder.terminalNodes.get(k).getOffset() < lastIfStatementOffset) {
							break;
						} else {
							Builder.terminalNodes.get(k).addChild(nextNode);
							nextNode.addParent(Builder.terminalNodes.get(k));
							Builder.terminalNodes.remove(k);
						}
					}
					Builder.terminalNodes.add(nextNode);
				}
				endStatementIndex = -1;	//重新初始化endStatementIndex=-1
				firstSequenceStatementIndex = -1; //重新初始化firstSequenceStatementIndex=-1				
			}else{
				 /*******************************************************
				 * if分支和else分支均为单条语句						    *
				 *******************************************************/
				//if分支
				if(iais.getChildren()[1] instanceof IASTIfStatement){
					IASTBinaryExpression iabe = (IASTBinaryExpression)iais.getChildren()[1].getChildren()[0];
					int offsetTmp = iabe.getFileLocation().getNodeOffset();
					ifNode.setBinaryExpression(iabe);
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(3);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					parent.setIfChild(ifNode);
					ifNode.addParent(parent);
					
					addNode(ifNode, (IASTIfStatement)iais.getChildren()[1]);
				}else if(iais.getChildren()[1] instanceof IASTForStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					Builder.terminalNodes.add(Builder.currentNode);
					CFGNode forNode = Builder.currentNode;
					ForStatementVisitor forVisitor = new ForStatementVisitor();
					forVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(forVisitor);
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
				}else if(iais.getChildren()[1] instanceof IASTWhileStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					Builder.terminalNodes.add(Builder.currentNode);
					CFGNode whileNode = Builder.currentNode;
					WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
					whileVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(whileVisitor);
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
				}else if(iais.getChildren()[1] instanceof IASTDoStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					
					DoStatementVisitor doVisitor = new DoStatementVisitor();
					doVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(doVisitor);
					//处理break节点
					if(Builder.breakNodes.size()>0){
						for(CFGNode breakNode : Builder.breakNodes){
							Builder.terminalNodes.add(breakNode);
						}
					}
					Builder.breakNodes = new ArrayList<CFGNode>();
				}else if(iais.getChildren()[1] instanceof IASTReturnStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(4);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
				}else if(iais.getChildren()[1] instanceof IASTBreakStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(7);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
					Builder.breakNodes.add(ifNode);
				}else if(iais.getChildren()[1] instanceof IASTContinueStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(8);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
					Builder.continueNodes.add(ifNode);
				}else{
					int ifOffset = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(ifOffset);
					ifNode.setSign(0);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					parent.setIfChild(ifNode);
					ifNode.addParent(parent);				
					Builder.terminalNodes.add(ifNode);
				}
				
				//else分支
				if(iais.getChildren()[2] instanceof IASTIfStatement){
					IASTBinaryExpression iabe = (IASTBinaryExpression)iais.getChildren()[2].getChildren()[0];
					int offsetTmp = iabe.getFileLocation().getNodeOffset();
					elseNode.setBinaryExpression(iabe);
					elseNode.setOffset(offsetTmp);
					elseNode.setSign(3);
					Builder.nodeNumber++;
					elseNode.setNodeNumber(Builder.nodeNumber);
					parent.setElseChild(elseNode);
					elseNode.addParent(parent);	
					
					addNode(elseNode, (IASTIfStatement)iais.getChildren()[2]);
				}else if(iais.getChildren()[2] instanceof IASTForStatement){
					Builder.currentNode = new CFGNode();
					parent.setElseChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);					
					Builder.terminalNodes.add(Builder.currentNode);		
					CFGNode forNode = Builder.currentNode;
					ForStatementVisitor forVisitor = new ForStatementVisitor();
					forVisitor.shouldVisitStatements = true;
					iais.getChildren()[2].accept(forVisitor);
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
				}else if(iais.getChildren()[2] instanceof IASTWhileStatement){
					Builder.currentNode = new CFGNode();
					parent.setElseChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);					
					Builder.terminalNodes.add(Builder.currentNode);	
					CFGNode whileNode = Builder.currentNode;
					WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
					whileVisitor.shouldVisitStatements = true;
					iais.getChildren()[2].accept(whileVisitor);
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
				}else if(iais.getChildren()[2] instanceof IASTDoStatement){
					Builder.currentNode = new CFGNode();
					parent.setElseChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);		
					
					DoStatementVisitor doVisitor = new DoStatementVisitor();
					doVisitor.shouldVisitStatements = true;
					iais.getChildren()[2].accept(doVisitor);
					//处理break节点
					if(Builder.breakNodes.size()>0){
						for(CFGNode breakNode : Builder.breakNodes){
							Builder.terminalNodes.add(breakNode);
						}
					}
					Builder.breakNodes = new ArrayList<CFGNode>();
				}else if(iais.getChildren()[2] instanceof IASTReturnStatement){
					 int offsetTmp = iais.getChildren()[2].getFileLocation().getNodeOffset();
					 elseNode.setOffset(offsetTmp);
					 elseNode.setSign(4);
					 Builder.nodeNumber++;
					 elseNode.setNodeNumber(Builder.nodeNumber);
					 elseNode.addParent(parent);
					 parent.setElseChild(elseNode);
				}else if(iais.getChildren()[2] instanceof IASTBreakStatement){
					int offsetTmp = iais.getChildren()[2].getFileLocation().getNodeOffset();
					 elseNode.setOffset(offsetTmp);
					 elseNode.setSign(7);
					 Builder.nodeNumber++;
					 elseNode.setNodeNumber(Builder.nodeNumber);
					 elseNode.addParent(parent);
					 parent.setElseChild(elseNode);
					 Builder.breakNodes.add(elseNode);
				}else if(iais.getChildren()[2] instanceof IASTContinueStatement){
					int offsetTmp = iais.getChildren()[2].getFileLocation().getNodeOffset();
					 elseNode.setOffset(offsetTmp);
					 elseNode.setSign(8);
					 Builder.nodeNumber++;
					 elseNode.setNodeNumber(Builder.nodeNumber);
					 elseNode.addParent(parent);
					 parent.setElseChild(elseNode);
					 Builder.continueNodes.add(elseNode);
				}else{
					int elseOffset = iais.getChildren()[2].getFileLocation().getNodeOffset();
					elseNode.setOffset(elseOffset);
					elseNode.setSign(1);
					Builder.nodeNumber++;
					elseNode.setNodeNumber(Builder.nodeNumber);
					parent.setElseChild(elseNode);
					elseNode.addParent(parent);
					Builder.terminalNodes.add(elseNode);
				}	
			}
		} else {
/****************************只有if语句，没有else语句********************************************/
			if(iais.getChildren()[1] instanceof IASTCompoundStatement){
				 /*******************************************************
				 * if分支为IASTCompoundStatement						    *
				 *******************************************************/
				IASTCompoundStatement ifCompound = (IASTCompoundStatement) iais.getChildren()[1];
	
				// 处理if分支
				IASTNode[] ifCompoundChildren = ifCompound.getChildren();
				int ifCompoundChildrenN = ifCompoundChildren.length;
				if(ifCompoundChildrenN>0){
					if(ifCompoundChildren[0] instanceof IASTIfStatement){ }            //if-else
					else if(ifCompoundChildren[0] instanceof IASTForStatement){ }      //for
					else if(ifCompoundChildren[0] instanceof IASTWhileStatement){ }    //while
					else if(ifCompoundChildren[0] instanceof IASTDoStatement){ }       //do-while
					else if(ifCompoundChildren[0] instanceof IASTReturnStatement){ }   //return
					else if(ifCompoundChildren[0] instanceof IASTBreakStatement){ }    //break
					else if(ifCompoundChildren[0] instanceof IASTContinueStatement){ } //continue
					else{
						int offsetTmp = ifCompound.getFileLocation().getNodeOffset();
						ifNode.setSign(0);
						ifNode.setOffset(offsetTmp);
						Builder.nodeNumber++;
						ifNode.setNodeNumber(Builder.nodeNumber);
						parent.setIfChild(ifNode);
						ifNode.addParent(parent);
					}
				}
				int countI = 0;
				int firstSequenceStatementIndex = -1;
				for (int i = 0; i < ifCompoundChildrenN; i++) {
					if(ifCompoundChildren[i] instanceof IASTReturnStatement) {
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "return语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(4);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
						    }else{
						    	 CFGNode returnNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 returnNode.setOffset(offsetTmp);
								 returnNode.setSign(4);
								 Builder.nodeNumber++;
								 returnNode.setNodeNumber(Builder.nodeNumber);
								 returnNode.addParent(ifNode);
								 ifNode.addChild(returnNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode returnNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(returnNode);
									    returnNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode returnNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    returnNode.setOffset(offsetTmp);
							    returnNode.setSign(4);
							    Builder.nodeNumber++;
							    returnNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(returnNode);
							    returnNode.addParent(nextNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTBreakStatement) {
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "break语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(7);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
							    Builder.breakNodes.add(ifNode);
						    }else{
						    	 CFGNode breakNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 breakNode.setOffset(offsetTmp);
								 breakNode.setSign(7);
								 Builder.nodeNumber++;
								 breakNode.setNodeNumber(Builder.nodeNumber);
								 breakNode.addParent(ifNode);
								 ifNode.addChild(breakNode);
								 Builder.breakNodes.add(breakNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode breakNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    Builder.breakNodes.add(breakNode);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(breakNode);
									    breakNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode breakNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    breakNode.setOffset(offsetTmp);
							    breakNode.setSign(7);
							    Builder.nodeNumber++;
							    breakNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(breakNode);
							    breakNode.addParent(nextNode);
							    Builder.breakNodes.add(breakNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTContinueStatement) {
						countI++; 					
						endStatementIndex = i;
						if(i!=ifCompoundChildrenN-1){
							JOptionPane.showMessageDialog(null, "continue语句位置存在错误！");
						}
						if(countI==1){
							if(i==0){
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    ifNode.setOffset(offsetTmp);
							    ifNode.setSign(8);
							    Builder.nodeNumber++;
							    ifNode.setNodeNumber(Builder.nodeNumber);
							    ifNode.addParent(parent);
							    parent.setIfChild(ifNode);
							    Builder.continueNodes.add(ifNode);
						    }else{
						    	 CFGNode continueNode = new CFGNode();
								 int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
								 continueNode.setOffset(offsetTmp);
								 continueNode.setSign(8);
								 Builder.nodeNumber++;
								 continueNode.setNodeNumber(Builder.nodeNumber);
								 continueNode.addParent(ifNode);
								 ifNode.addChild(continueNode);
								 Builder.continueNodes.add(continueNode);
							}
						}else{
							if(firstSequenceStatementIndex == (ifCompoundChildrenN-1)){
							    CFGNode continueNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    continueNode.setOffset(offsetTmp);
							    continueNode.setSign(8);
							    Builder.nodeNumber++;
							    continueNode.setNodeNumber(Builder.nodeNumber);
							    Builder.continueNodes.add(continueNode);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(continueNode);
									    continueNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }
						    }else{
							    CFGNode nextNode = new CFGNode();
							    int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							    nextNode.setOffset(offsetTmp);
							    nextNode.setSign(-1);
							    Builder.nodeNumber++;
							    nextNode.setNodeNumber(Builder.nodeNumber);
							    //为当前顺序节点添加父节点
							    int terminalNodesNum = Builder.terminalNodes.size();
							    for(int k=(terminalNodesNum-1); k>-1; k--){
								    if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
									    Builder.terminalNodes.get(k).addChild(nextNode);
									    nextNode.addParent(Builder.terminalNodes.get(k));
									    Builder.terminalNodes.remove(k);
								    }
							    }							
							    CFGNode continueNode = new CFGNode();
							    offsetTmp = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							    continueNode.setOffset(offsetTmp);
							    continueNode.setSign(8);
							    Builder.nodeNumber++;
							    continueNode.setNodeNumber(Builder.nodeNumber);
							    nextNode.addChild(continueNode);
							    continueNode.addParent(nextNode);
							    Builder.continueNodes.add(continueNode);
						    }
						}
					}else if(ifCompoundChildren[i] instanceof IASTIfStatement) {
						countI++; // 标记该if分支内是否有if-else语句						
						endStatementIndex = i;

						if (countI == 1) { // 当前if分支内有且仅有一个if-else语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							IASTBinaryExpression iabe = (IASTBinaryExpression) ifCompoundChildren[i].getChildren()[0];
							int offsetTmp = iabe.getFileLocation().getNodeOffset();
							if(i==0){
								ifNode.setBinaryExpression(iabe);
								ifNode.setOffset(offsetTmp);
								ifNode.setSign(3);
								Builder.nodeNumber++;
								ifNode.setNodeNumber(Builder.nodeNumber);
								parent.setIfChild(ifNode);
								ifNode.addParent(parent);
	
								addNode(ifNode,(IASTIfStatement) ifCompoundChildren[i]);
							}else{
								CFGNode nextNode = new CFGNode();
								nextNode.setBinaryExpression(iabe);
								nextNode.setOffset(offsetTmp);
								nextNode.setSign(3);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								ifNode.addChild(nextNode);
								nextNode.addParent(ifNode);
								
								addNode(nextNode, (IASTIfStatement)ifCompoundChildren[i]);
							}
						} else { // 当前if分支内有2个或2个以上的if-else语句
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression)ifCompoundChildren[i].getChildren()[0];
								offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								nextNode.addChild(nextIfNode);
								nextIfNode.addParent(nextNode);
								
								addNode(nextIfNode, (IASTIfStatement)ifCompoundChildren[i]);
							}else{
								CFGNode nextIfNode = new CFGNode();
								IASTBinaryExpression iabe = (IASTBinaryExpression) ifCompoundChildren[i].getChildren()[0];
								int offsetTmp = iabe.getFileLocation().getNodeOffset();
								nextIfNode.setBinaryExpression(iabe);
								nextIfNode.setOffset(offsetTmp);
								nextIfNode.setSign(3);
								Builder.nodeNumber++;
								nextIfNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextIfNode);
										nextIfNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								addNode(nextIfNode, (IASTIfStatement)ifCompoundChildren[i]);
							}
						}
						firstSequenceStatementIndex = i+1;
					}else if(ifCompoundChildren[i] instanceof IASTForStatement){
						countI++; // 标记该if分支内是否有if-else语句
						
						endStatementIndex = i;
                        CFGNode forNode;
						if (countI == 1) { // 当前if分支内有且仅有一个for语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}
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
						} else { // 当前if分支内有2个或2个以上的if-else语句或者for语句
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								forNode = Builder.currentNode;
								ForStatementVisitor forVisitor = new ForStatementVisitor();
								forVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(forVisitor);
							}
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
					}else if(ifCompoundChildren[i] instanceof IASTWhileStatement){
						countI++;						
						endStatementIndex = i;
                        CFGNode whileNode;
						if (countI == 1) { // 当前if分支内有且仅有一个while语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();
							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}
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
						} else {
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.terminalNodes.add(Builder.currentNode);
								whileNode = Builder.currentNode;
								WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
								whileVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(whileVisitor);
							}
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
					}else if(ifCompoundChildren[i] instanceof IASTDoStatement){
						countI++; 						
						endStatementIndex = i;

						if (countI == 1) { // 当前if分支内有且仅有一个do-while语句
							lastIfStatementOffset = ifCompoundChildren[i].getFileLocation().getNodeOffset();

							if(i==0){
								Builder.currentNode = new CFGNode();
								parent.setIfChild(Builder.currentNode);
								Builder.currentNode.addParent(parent);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								ifNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(ifNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}
							//处理break节点
							if(Builder.breakNodes.size()>0){
								for(CFGNode breakNode : Builder.breakNodes){
									Builder.terminalNodes.add(breakNode);
								}
							}
							Builder.breakNodes = new ArrayList<CFGNode>();
							firstSequenceStatementIndex = i+1;
						} else { 
							if(firstSequenceStatementIndex != i){
								CFGNode nextNode = new CFGNode();
								int offsetTmp = ifCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
								nextNode.setSign(-1);
								nextNode.setOffset(offsetTmp);
								Builder.nodeNumber++;
								nextNode.setNodeNumber(Builder.nodeNumber);
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(nextNode);
										nextNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								Builder.currentNode = new CFGNode();
								nextNode.addChild(Builder.currentNode);
								Builder.currentNode.addParent(nextNode);
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}else{
								Builder.currentNode = new CFGNode();
								int terminalNodesNum = Builder.terminalNodes.size();
								for(int k=(terminalNodesNum-1); k>-1; k--){
									if(Builder.terminalNodes.get(k).getOffset() >= lastIfStatementOffset){
										Builder.terminalNodes.get(k).addChild(Builder.currentNode);
										Builder.currentNode.addParent(Builder.terminalNodes.get(k));
										Builder.terminalNodes.remove(k);
									}
								}
								
								DoStatementVisitor doVisitor = new DoStatementVisitor();
								doVisitor.shouldVisitStatements = true;
								ifCompoundChildren[i].accept(doVisitor);
							}
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
				}
				if (countI == 0) { // countI==0表明：当前if分支块中没有if-else语句
					Builder.terminalNodes.add(ifNode);
				}
				//当前块中最后的语句是顺序语句
				if(endStatementIndex > -1 && endStatementIndex < ifCompoundChildrenN-1){
					CFGNode nextNode = new CFGNode();
					int offsetTmp = ifCompoundChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
					nextNode.setOffset(offsetTmp);
					nextNode.setSign(-1);
					Builder.nodeNumber++;
					nextNode.setNodeNumber(Builder.nodeNumber);
					
					// 为当前顺序节点添加父节点
					int terminalNodesNum = Builder.terminalNodes.size();
					for (int k = (terminalNodesNum - 1); k > -1; k--) {
						if (Builder.terminalNodes.get(k).getOffset() < lastIfStatementOffset) {
							break;
						} else {
							Builder.terminalNodes.get(k).addChild(nextNode);
							nextNode.addParent(Builder.terminalNodes.get(k));
							Builder.terminalNodes.remove(k);
						}
					}
					Builder.terminalNodes.add(nextNode);
				}
				endStatementIndex = -1;	//重新初始化endStatementIndex=-1
				firstSequenceStatementIndex = -1; //重新初始化firstSequenceStatementIndex=-1
				
				//if分支的条件语句不满足时
				Builder.terminalNodes.add(parent);
				
			}else{
				 /*******************************************************
				 * if分支为单条语句		            				    *
				 *******************************************************/
				if(iais.getChildren()[1] instanceof IASTIfStatement){
					IASTBinaryExpression iabe = (IASTBinaryExpression)iais.getChildren()[1].getChildren()[0];
					int offsetTmp = iabe.getFileLocation().getNodeOffset();
					ifNode.setBinaryExpression(iabe);
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(3);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					parent.setIfChild(ifNode);
					ifNode.addParent(parent);
					
					addNode(ifNode, (IASTIfStatement)iais.getChildren()[1]);
				}else if(iais.getChildren()[1] instanceof IASTForStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					Builder.terminalNodes.add(Builder.currentNode);
					CFGNode forNode = Builder.currentNode;
					ForStatementVisitor forVisitor = new ForStatementVisitor();
					forVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(forVisitor);
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
				}else if(iais.getChildren()[1] instanceof IASTWhileStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					Builder.terminalNodes.add(Builder.currentNode);
					CFGNode whileNode = Builder.currentNode;
					WhileStatementVisitor whileVisitor = new WhileStatementVisitor();
					whileVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(whileVisitor);
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
				}else if(iais.getChildren()[1] instanceof IASTDoStatement){
					Builder.currentNode = new CFGNode();
					parent.setIfChild(Builder.currentNode);
					Builder.currentNode.addParent(parent);				
					
					DoStatementVisitor doVisitor = new DoStatementVisitor();
					doVisitor.shouldVisitStatements = true;
					iais.getChildren()[1].accept(doVisitor);
					//处理break节点
					if(Builder.breakNodes.size()>0){
						for(CFGNode breakNode : Builder.breakNodes){
							Builder.terminalNodes.add(breakNode);
						}
					}
					Builder.breakNodes = new ArrayList<CFGNode>();
				}else if(iais.getChildren()[1] instanceof IASTReturnStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(4);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
				}else if(iais.getChildren()[1] instanceof IASTBreakStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(7);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
					Builder.breakNodes.add(ifNode);
				}else if(iais.getChildren()[1] instanceof IASTContinueStatement){
					int offsetTmp = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(offsetTmp);
					ifNode.setSign(8);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					ifNode.addParent(parent);
					parent.setIfChild(ifNode);
					Builder.continueNodes.add(ifNode);
				}else{
					int ifOffset = iais.getChildren()[1].getFileLocation().getNodeOffset();
					ifNode.setOffset(ifOffset);
					ifNode.setSign(0);
					Builder.nodeNumber++;
					ifNode.setNodeNumber(Builder.nodeNumber);
					parent.setIfChild(ifNode);
					ifNode.addParent(parent);				
					Builder.terminalNodes.add(ifNode);
				}
				
				//当if分支的条件语句不满足时
				Builder.terminalNodes.add(parent);
			}
		}
	}
}
