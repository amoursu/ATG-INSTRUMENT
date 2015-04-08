package cn.nju.seg.atg.visitor;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import cn.nju.seg.atg.parse.Builder;

/**
 * a visitor for do-while statement in C or C++ programs.
 * @author zy
 *
 */
public class DoStatementVisitor extends ASTVisitor {
    public DoStatementVisitor(){  }
    
    public int visit(IASTStatement node) {
    	if (node instanceof IASTDoStatement) {
    		IASTDoStatement iads = (IASTDoStatement)node;
			IASTNode[] iafsChildren = iads.getChildren();		
			if(iafsChildren[1] instanceof IASTBinaryExpression){
				CFGNode whileNode = addNode(Builder.currentNode, iads);
				Builder.terminalNodes.add(whileNode);
				//处理continue节点
				if(Builder.continueNodes.size()>0){
					for(CFGNode continueNode : Builder.continueNodes){
						whileNode.addParent(continueNode);
						continueNode.addChild(whileNode);
					}
				}
				Builder.continueNodes = new ArrayList<CFGNode>();
			}else{
				JOptionPane.showMessageDialog(null, "当前do-while循环的格式无法识别！");
			}
			
			return PROCESS_ABORT;
		}

		return PROCESS_CONTINUE;
    }
    
    public CFGNode addNode(CFGNode doNode, IASTDoStatement iads) {
		int lastDoStatementOffset = -1;
		int endStatementIndex = -1;
		
		IASTBinaryExpression iabe = (IASTBinaryExpression)iads.getChildren()[1];
		int offsetTmp = iabe.getFileLocation().getNodeOffset();
		CFGNode whileNode = new CFGNode();
		whileNode.setSign(6);
		whileNode.setBinaryExpression(iabe);
		whileNode.setOffset(offsetTmp);
		
		if(iads.getChildren()[0] instanceof IASTCompoundStatement){
			/***************************
			 * do-while循环主体是复合语句 *
			 **************************/
			IASTCompoundStatement doCompound = (IASTCompoundStatement)iads.getChildren()[0];
			
			IASTNode[] doCompoundChildren = doCompound.getChildren();
			int doCompoundChildrenN = doCompoundChildren.length;
			if(doCompoundChildrenN>0){
				if(doCompoundChildren[0] instanceof IASTDoStatement){
					//空语句
				}else if(doCompoundChildren[0] instanceof IASTForStatement){
					//空语句
				}else if(doCompoundChildren[0] instanceof IASTIfStatement){
					//空语句
				}else{
					//循环体第一个节点为current_node
					offsetTmp = doCompound.getFileLocation().getNodeOffset();
					doNode.setSign(-1);
					doNode.setOffset(offsetTmp);
					Builder.nodeNumber++;
					doNode.setNodeNumber(Builder.nodeNumber);
					whileNode.addChild(doNode);
					doNode.addParent(whileNode);
				}
			}
			int countI=0;
			int firstSequenceStatementIndex = -1;
			for(int i=0; i<doCompoundChildrenN; i++){
				//嵌套do-while循环
				if(doCompoundChildren[i] instanceof IASTDoStatement){
					countI++;					
					endStatementIndex = i;
					
					//while节点添加do节点其为子节点
					whileNode.addChild(doNode);
					doNode.addParent(whileNode);
					
					if(countI == 1){
						lastDoStatementOffset = doCompoundChildren[i].getFileLocation().getNodeOffset();
						
						if(i==0){							
							CFGNode nextWhileNode = addNode(doNode, (IASTDoStatement)doCompoundChildren[i]);
							Builder.terminalNodes.add(nextWhileNode);
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
									nextWhileNode.addParent(continueNode);
									continueNode.addChild(nextWhileNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
						}else{
							CFGNode nextDoNode = new CFGNode();	
							doNode.addChild(nextDoNode);
							nextDoNode.addParent(doNode);
							CFGNode nextWhileNode = addNode(nextDoNode, (IASTDoStatement)doCompoundChildren[i]);
							Builder.terminalNodes.add(nextWhileNode);
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
									nextWhileNode.addParent(continueNode);
									continueNode.addChild(nextWhileNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
						}
						firstSequenceStatementIndex = i+1;
					}else{// 当前do-while分支内有2个或2个以上的控制语句
						if(firstSequenceStatementIndex != i){
							//控制语句之间存在顺序语句
							CFGNode nextNode = new CFGNode();
							offsetTmp = doCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							nextNode.setSign(-1);
							nextNode.setOffset(offsetTmp);
							Builder.nodeNumber++;
							nextNode.setNodeNumber(Builder.nodeNumber);
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastDoStatementOffset){
									Builder.terminalNodes.get(k).addChild(nextNode);
									nextNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							CFGNode nextDoNode = new CFGNode();
							nextNode.addChild(nextDoNode);
							nextDoNode.addParent(nextNode);
							CFGNode nextWhileNode = addNode(nextDoNode, (IASTDoStatement)doCompoundChildren[i]);
							Builder.terminalNodes.add(nextWhileNode);
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
									nextWhileNode.addParent(continueNode);
									continueNode.addChild(nextWhileNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
						}else{
							CFGNode nextDoNode = new CFGNode();
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastDoStatementOffset){
									Builder.terminalNodes.get(k).addChild(nextDoNode);
									nextDoNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							CFGNode nextWhileNode = addNode(nextDoNode, (IASTDoStatement)doCompoundChildren[i]);
							Builder.terminalNodes.add(nextWhileNode);	
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
									nextWhileNode.addParent(continueNode);
									continueNode.addChild(nextWhileNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
						}
						firstSequenceStatementIndex = i+1;
					}
				}else if(doCompoundChildren[i] instanceof IASTIfStatement){
					countI++;					
					endStatementIndex = i;
					
					if(countI == 1){
						lastDoStatementOffset = doCompoundChildren[i].getFileLocation().getNodeOffset();

						if(i==0){
							whileNode.addChild(Builder.currentNode);
							Builder.currentNode.addParent(whileNode);
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							doCompoundChildren[i].accept(ifStatementVisitor);
						}else{
							Builder.currentNode = new CFGNode();
							doNode.addChild(Builder.currentNode);
							Builder.currentNode.addParent(doNode);
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							doCompoundChildren[i].accept(ifStatementVisitor);
						}
						firstSequenceStatementIndex = i+1;
					}else{	// 当前do-while分支内有2个或2个以上的控制语句
						if(firstSequenceStatementIndex != i){
							//控制语句之间存在顺序语句
							CFGNode nextNode = new CFGNode();
							offsetTmp = doCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							nextNode.setSign(-1);
							nextNode.setOffset(offsetTmp);
							Builder.nodeNumber++;
							nextNode.setNodeNumber(Builder.nodeNumber);
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastDoStatementOffset){
									Builder.terminalNodes.get(k).addChild(nextNode);
									nextNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							Builder.currentNode = new CFGNode();
							nextNode.addChild(Builder.currentNode);
							Builder.currentNode.addParent(nextNode);
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							doCompoundChildren[i].accept(ifStatementVisitor);
						}else{
							Builder.currentNode = new CFGNode();
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastDoStatementOffset){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							doCompoundChildren[i].accept(ifStatementVisitor);
						}
						firstSequenceStatementIndex = i+1;
					}
				}
			}
			if(countI == 0){
				doNode.addChild(whileNode);
				whileNode.addParent(doNode);
			}
			//当前块中最后的语句是顺序语句
			if(endStatementIndex > -1 && endStatementIndex < doCompoundChildrenN-1){
				CFGNode nextNode = new CFGNode();
				offsetTmp = doCompoundChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
				nextNode.setOffset(offsetTmp);
				nextNode.setSign(-1);
				Builder.nodeNumber++;
				nextNode.setNodeNumber(Builder.nodeNumber);
				
				//为当前顺序节点添加父节点
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastDoStatementOffset){
						Builder.terminalNodes.get(k).addChild(nextNode);
						nextNode.addParent(Builder.terminalNodes.get(k));
						Builder.terminalNodes.remove(k);
					}
				}
				
				nextNode.addChild(whileNode);
				whileNode.addParent(nextNode);
			}
			//最后的语句是控制语句
			if(doCompoundChildren[doCompoundChildrenN-1] instanceof IASTDoStatement || doCompoundChildren[doCompoundChildrenN-1] instanceof IASTIfStatement){
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastDoStatementOffset){
						Builder.terminalNodes.get(k).addChild(whileNode);
						whileNode.addParent(Builder.terminalNodes.get(k));
						Builder.terminalNodes.remove(k);
					}
				}
			}
			endStatementIndex = -1;
			firstSequenceStatementIndex = -1;
		}else{
			/*************************
			 * while循环主体是单条语句	 *
			 ************************/
			if(iads.getChildren()[0] instanceof IASTDoStatement){
				//while节点添加do节点其为子节点
				whileNode.addChild(doNode);
				doNode.addParent(whileNode);
				CFGNode nextWhileNode = addNode(doNode, (IASTDoStatement)iads.getChildren()[0]);
				//嵌套的do-while节点添加while节点为父节点
				nextWhileNode.addChild(whileNode);
				whileNode.addParent(nextWhileNode);
				//处理break节点
				if(Builder.breakNodes.size()>0){
					for(CFGNode breakNode : Builder.breakNodes){
						breakNode.addChild(whileNode);
						whileNode.addParent(breakNode);
					}
				}
				Builder.breakNodes = new ArrayList<CFGNode>();
				//处理continue节点
				if(Builder.continueNodes.size()>0){
					for(CFGNode continueNode : Builder.continueNodes){
						nextWhileNode.addParent(continueNode);
						continueNode.addChild(nextWhileNode);
					}
				}
				Builder.continueNodes = new ArrayList<CFGNode>();
			}else if(iads.getChildren()[0] instanceof IASTIfStatement){
				lastDoStatementOffset = iads.getChildren()[0].getFileLocation().getNodeOffset();
				Builder.currentNode = new CFGNode();
				whileNode.addChild(Builder.currentNode);
				Builder.currentNode.addParent(whileNode);
				
				IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
				ifStatementVisitor.shouldVisitStatements = true;
				iads.getChildren()[0].accept(ifStatementVisitor);
				
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastDoStatementOffset){
						Builder.terminalNodes.get(k).addChild(whileNode);
						whileNode.addParent(Builder.terminalNodes.get(k));
						Builder.terminalNodes.remove(k);
					}
				}
			}else{
				offsetTmp = iads.getChildren()[0].getFileLocation().getNodeOffset();
				doNode.setOffset(offsetTmp);
				doNode.setSign(6);
				Builder.nodeNumber++;
				doNode.setNodeNumber(Builder.nodeNumber);
				whileNode.addChild(doNode);
				doNode.addParent(whileNode);
				whileNode.addParent(doNode);
				doNode.addChild(whileNode);
			}
		}
		
		Builder.nodeNumber++;
		whileNode.setNodeNumber(Builder.nodeNumber);
		return whileNode;
	}
}
