package cn.nju.seg.atg.visitor;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import cn.nju.seg.atg.parse.Builder;

/**
 * a visitor for while statement in C or C++ programs.
 * @author zy
 *
 */
public class WhileStatementVisitor extends ASTVisitor {
    public WhileStatementVisitor(){  }
    
    public int visit(IASTStatement node) {
    	if (node instanceof IASTWhileStatement) {
			IASTWhileStatement iafs = (IASTWhileStatement)node;
			IASTNode[] iafsChildren = iafs.getChildren();		
			if(iafsChildren[0] instanceof IASTBinaryExpression){
				IASTBinaryExpression iabe = (IASTBinaryExpression)iafsChildren[0];
				int offsetTmp = iabe.getFileLocation().getNodeOffset();
				Builder.currentNode.setSign(5);
				Builder.currentNode.setBinaryExpression(iabe);
				Builder.currentNode.setOffset(offsetTmp);
				Builder.nodeNumber++;
				Builder.currentNode.setNodeNumber(Builder.nodeNumber);
				
				addNode(Builder.currentNode, iafs);
			}else{
				JOptionPane.showMessageDialog(null, "当前while循环的格式无法识别！");
			}
			
			return PROCESS_ABORT;
		}

		return PROCESS_CONTINUE;
    }
    
    public void addNode(CFGNode parent, IASTWhileStatement iaws) {
		CFGNode whileNode = new CFGNode();
		int lastWhileStatementOffset = -1;
		int endStatementIndex = -1;
		
		if(iaws.getChildren()[1] instanceof IASTCompoundStatement){
			/*************************
			 * while循环主体是复合语句	 *
			 ************************/
			IASTCompoundStatement whileCompound = (IASTCompoundStatement)iaws.getChildren()[1];
			
			IASTNode[] whileCompoundChildren = whileCompound.getChildren();
			int whileCompoundChildrenN = whileCompoundChildren.length;
			if(whileCompoundChildrenN>0){
				if(whileCompoundChildren[0] instanceof IASTWhileStatement){
					//空语句
				}else if(whileCompoundChildren[0] instanceof IASTForStatement){
					//空语句
				}else if(whileCompoundChildren[0] instanceof IASTIfStatement){
					//空语句
				}else{
					int offsetTmp = whileCompound.getFileLocation().getNodeOffset();
					whileNode.setSign(-1);
					whileNode.setOffset(offsetTmp);
					Builder.nodeNumber++;
					whileNode.setNodeNumber(Builder.nodeNumber);
					parent.addChild(whileNode);
					whileNode.addParent(parent);
				}
			}
			int countI=0;
			int firstSequenceStatementIndex = -1;
			for(int i=0; i<whileCompoundChildrenN; i++){
				//嵌套While循环
				if(whileCompoundChildren[i] instanceof IASTWhileStatement){
					countI++;					
					endStatementIndex = i;
					
					if(countI == 1){
						lastWhileStatementOffset = whileCompoundChildren[i].getFileLocation().getNodeOffset();
						
						IASTBinaryExpression iabe = (IASTBinaryExpression)whileCompoundChildren[i].getChildren()[0];
						int offsetTmp = iabe.getFileLocation().getNodeOffset();
						if(i==0){
							whileNode.setBinaryExpression(iabe);
							whileNode.setOffset(offsetTmp);
							whileNode.setSign(5);
							Builder.nodeNumber++;
							whileNode.setNodeNumber(Builder.nodeNumber);
							parent.addChild(whileNode);
							whileNode.addParent(parent);
							
							Builder.terminalNodes.add(whileNode);							
							addNode(whileNode, (IASTWhileStatement)whileCompoundChildren[i]);
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
						}else{
							CFGNode nextNode = new CFGNode();
							nextNode.setBinaryExpression(iabe);
							nextNode.setOffset(offsetTmp);
							nextNode.setSign(5);
							Builder.nodeNumber++;
							nextNode.setNodeNumber(Builder.nodeNumber);
							whileNode.addChild(nextNode);
							nextNode.addParent(whileNode);
							
							Builder.terminalNodes.add(nextNode);						
							addNode(nextNode, (IASTWhileStatement)whileCompoundChildren[i]);
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
									nextNode.addParent(continueNode);
									continueNode.addChild(nextNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
						}
						firstSequenceStatementIndex = i+1;
					}else{// 当前while分支内有2个或2个以上的控制语句
						if(firstSequenceStatementIndex != i){
							//控制语句之间存在顺序语句
							CFGNode nextNode = new CFGNode();
							int offsetTmp = whileCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							nextNode.setSign(-1);
							nextNode.setOffset(offsetTmp);
							Builder.nodeNumber++;
							nextNode.setNodeNumber(Builder.nodeNumber);
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastWhileStatementOffset){
									Builder.terminalNodes.get(k).addChild(nextNode);
									nextNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							CFGNode nextWhileNode = new CFGNode();
							IASTBinaryExpression iabe = (IASTBinaryExpression)whileCompoundChildren[i].getChildren()[0];
							offsetTmp = iabe.getFileLocation().getNodeOffset();
							nextWhileNode.setBinaryExpression(iabe);
							nextWhileNode.setOffset(offsetTmp);
							nextWhileNode.setSign(5);
							Builder.nodeNumber++;
							nextWhileNode.setNodeNumber(Builder.nodeNumber);
							nextNode.addChild(nextWhileNode);
							nextWhileNode.addParent(nextNode);
							
							Builder.terminalNodes.add(nextWhileNode);							
							addNode(nextWhileNode, (IASTWhileStatement)whileCompoundChildren[i]);
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
							CFGNode nextWhileNode = new CFGNode();
							IASTBinaryExpression iabe = (IASTBinaryExpression)whileCompoundChildren[i].getChildren()[1];
							int offsetTmp = iabe.getFileLocation().getNodeOffset();
							nextWhileNode.setBinaryExpression(iabe);
							nextWhileNode.setOffset(offsetTmp);
							nextWhileNode.setSign(5);
							Builder.nodeNumber++;
							nextWhileNode.setNodeNumber(Builder.nodeNumber);
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastWhileStatementOffset){
									Builder.terminalNodes.get(k).addChild(nextWhileNode);
									nextWhileNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							Builder.terminalNodes.add(nextWhileNode);						
							addNode(nextWhileNode, (IASTWhileStatement)whileCompoundChildren[i]);
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
				}else if(whileCompoundChildren[i] instanceof IASTIfStatement){
					countI++;					
					endStatementIndex = i;
					
					if(countI == 1){
						lastWhileStatementOffset = whileCompoundChildren[i].getFileLocation().getNodeOffset();

						if(i==0){
							Builder.currentNode = new CFGNode();
							parent.addChild(Builder.currentNode);
							Builder.currentNode.addParent(parent);
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							whileCompoundChildren[i].accept(ifStatementVisitor);
						}else{
							Builder.currentNode = new CFGNode();
							whileNode.addChild(Builder.currentNode);
							Builder.currentNode.addParent(whileNode);
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							whileCompoundChildren[i].accept(ifStatementVisitor);
						}
						firstSequenceStatementIndex = i+1;
					}else{	// 当前while分支内有2个或2个以上的控制语句
						if(firstSequenceStatementIndex != i){
							CFGNode nextNode = new CFGNode();
							int offsetTmp = whileCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							nextNode.setSign(-1);
							nextNode.setOffset(offsetTmp);
							Builder.nodeNumber++;
							nextNode.setNodeNumber(Builder.nodeNumber);
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastWhileStatementOffset){
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
							whileCompoundChildren[i].accept(ifStatementVisitor);
						}else{
							Builder.currentNode = new CFGNode();
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastWhileStatementOffset){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							whileCompoundChildren[i].accept(ifStatementVisitor);
						}
						firstSequenceStatementIndex = i+1;
					}
				}
			}
			if(countI == 0){
				whileNode.addChild(parent);
				parent.addParent(whileNode);
			}
			//当前块中最后的语句是顺序语句
			if(endStatementIndex > -1 && endStatementIndex < whileCompoundChildrenN-1){
				CFGNode nextNode = new CFGNode();
				int offsetTmp = whileCompoundChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
				nextNode.setOffset(offsetTmp);
				nextNode.setSign(-1);
				Builder.nodeNumber++;
				nextNode.setNodeNumber(Builder.nodeNumber);
				
				//为当前顺序节点添加父节点
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastWhileStatementOffset){
						Builder.terminalNodes.get(k).addChild(nextNode);
						nextNode.addParent(Builder.terminalNodes.get(k));
						Builder.terminalNodes.remove(k);
					}
				}
				
				nextNode.addChild(parent);
				parent.addParent(nextNode);
			}
			if(whileCompoundChildren[whileCompoundChildrenN-1] instanceof IASTWhileStatement || whileCompoundChildren[whileCompoundChildrenN-1] instanceof IASTIfStatement){
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastWhileStatementOffset){
						Builder.terminalNodes.get(k).addChild(parent);
						parent.addParent(Builder.terminalNodes.get(k));
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
			if(iaws.getChildren()[1] instanceof IASTWhileStatement){
				IASTBinaryExpression iabe = (IASTBinaryExpression)iaws.getChildren()[1].getChildren()[0];
				int offsetTmp = iabe.getFileLocation().getNodeOffset();
				whileNode.setBinaryExpression(iabe);
				whileNode.setOffset(offsetTmp);
				whileNode.setSign(5);
				Builder.nodeNumber++;
				whileNode.setNodeNumber(Builder.nodeNumber);
				parent.addChild(whileNode);
				whileNode.addParent(parent);
				parent.addParent(whileNode);
				whileNode.addChild(parent);
				
				addNode(whileNode, (IASTWhileStatement)iaws.getChildren()[1]);
				//处理break节点
				if(Builder.breakNodes.size()>0){
					for(CFGNode breakNode : Builder.breakNodes){
						parent.addParent(breakNode);
						breakNode.addChild(parent);
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
			}else if(iaws.getChildren()[1] instanceof IASTIfStatement){
				lastWhileStatementOffset = iaws.getChildren()[1].getFileLocation().getNodeOffset();
				Builder.currentNode = new CFGNode();
				parent.addChild(Builder.currentNode);
				Builder.currentNode.addParent(parent);
				
				IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
				ifStatementVisitor.shouldVisitStatements = true;
				iaws.getChildren()[1].accept(ifStatementVisitor);
				
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastWhileStatementOffset){
						Builder.terminalNodes.get(k).addChild(parent);
						parent.addParent(Builder.terminalNodes.get(k));
						Builder.terminalNodes.remove(k);
					}
				}
			}else{
				int offsetTmp = iaws.getChildren()[1].getFileLocation().getNodeOffset();
				whileNode.setOffset(offsetTmp);
				whileNode.setSign(-1);
				Builder.nodeNumber++;
				whileNode.setNodeNumber(Builder.nodeNumber);
				parent.addChild(whileNode);
				whileNode.addParent(parent);
				parent.addParent(whileNode);
				whileNode.addChild(parent);
			}
		}
	}
}
