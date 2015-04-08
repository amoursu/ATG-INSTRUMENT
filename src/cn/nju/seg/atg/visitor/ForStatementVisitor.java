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

import cn.nju.seg.atg.parse.Builder;

/**
 * 
 * @author ChengXin
 * 
 */
public class ForStatementVisitor extends ASTVisitor {

	public ForStatementVisitor() {

	}

	public int visit(IASTStatement node) {
		if (node instanceof IASTForStatement) {
			IASTForStatement iafs = (IASTForStatement)node;     
			IASTNode[] iafsChildren = iafs.getChildren(); 
			if(iafsChildren[1] instanceof IASTBinaryExpression){
				IASTBinaryExpression iabe = (IASTBinaryExpression)iafsChildren[1];
				int offsetTmp = iabe.getFileLocation().getNodeOffset();
				Builder.currentNode.setSign(2);
				Builder.currentNode.setBinaryExpression(iabe);
				Builder.currentNode.setOffset(offsetTmp);
				Builder.nodeNumber++;
				Builder.currentNode.setNodeNumber(Builder.nodeNumber);
				
				addNode(Builder.currentNode, iafs);
			}else{
				JOptionPane.showMessageDialog(null, "当前for循环的格式无法识别！");
			}
			
			return PROCESS_ABORT;
		}

		return PROCESS_CONTINUE;
	}
	
	void addNode(CFGNode parent, IASTForStatement iafs){
		CFGNode forNode = new CFGNode();
		int lastForStatementOffset = -1;
		int endStatementIndex = -1;
		
		if(iafs.getChildren()[3] instanceof IASTCompoundStatement){
			/*************************
			 * for循环主体是复合语句	 *
			 ************************/
			IASTCompoundStatement forCompound = (IASTCompoundStatement)iafs.getChildren()[3];
			
			IASTNode[] forCompoundChildren = forCompound.getChildren();
			int forCompoundChildrenN = forCompoundChildren.length;
			if(forCompoundChildrenN >0){
				if(forCompoundChildren[0] instanceof IASTForStatement){ }
				else if(forCompoundChildren[0] instanceof IASTIfStatement){ }
				else{
					int offsetTmp = forCompound.getFileLocation().getNodeOffset();
					forNode.setSign(-1);
					forNode.setOffset(offsetTmp);
					Builder.nodeNumber++;
					forNode.setNodeNumber(Builder.nodeNumber);
					parent.addChild(forNode);
					forNode.addParent(parent);
				}
			}
			int countI=0;
			int firstSequenceStatementIndex = -1;
			for(int i=0; i<forCompoundChildrenN; i++){
				if(forCompoundChildren[i] instanceof IASTForStatement){
					countI++;
					
					endStatementIndex = i;
					
					if(countI == 1){
						lastForStatementOffset = forCompoundChildren[i].getFileLocation().getNodeOffset();
						
						IASTBinaryExpression iabe = (IASTBinaryExpression)forCompoundChildren[i].getChildren()[1];
						int offsetTmp = iabe.getFileLocation().getNodeOffset();
						if(i==0){
							forNode.setBinaryExpression(iabe);
							forNode.setOffset(offsetTmp);
							forNode.setSign(2);
							Builder.nodeNumber++;
							forNode.setNodeNumber(Builder.nodeNumber);
							parent.addChild(forNode);
							forNode.addParent(parent);
							
							Builder.terminalNodes.add(forNode);
							addNode(forNode, (IASTForStatement)forCompoundChildren[i]);
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
						}else{
							CFGNode nextNode = new CFGNode();
							nextNode.setBinaryExpression(iabe);
							nextNode.setOffset(offsetTmp);
							nextNode.setSign(2);
							Builder.nodeNumber++;
							nextNode.setNodeNumber(Builder.nodeNumber);
							forNode.addChild(nextNode);
							nextNode.addParent(forNode);
							
							Builder.terminalNodes.add(nextNode);							
							addNode(nextNode, (IASTForStatement)forCompoundChildren[i]);
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
					}else{	// 当前for分支内有2个或2个以上的for语句
						if(firstSequenceStatementIndex != i){
							CFGNode nextNode = new CFGNode();
							int offsetTmp = forCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							nextNode.setSign(-1);
							nextNode.setOffset(offsetTmp);
							Builder.nodeNumber++;
							nextNode.setNodeNumber(Builder.nodeNumber);
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastForStatementOffset){
									Builder.terminalNodes.get(k).addChild(nextNode);
									nextNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							CFGNode nextForNode = new CFGNode();
							IASTBinaryExpression iabe = (IASTBinaryExpression)forCompoundChildren[i].getChildren()[1];
							offsetTmp = iabe.getFileLocation().getNodeOffset();
							nextForNode.setBinaryExpression(iabe);
							nextForNode.setOffset(offsetTmp);
							nextForNode.setSign(2);
							Builder.nodeNumber++;
							nextForNode.setNodeNumber(Builder.nodeNumber);
							nextNode.addChild(nextForNode);
							nextForNode.addParent(nextNode);
							
							Builder.terminalNodes.add(nextForNode);							
							addNode(nextForNode, (IASTForStatement)forCompoundChildren[i]);
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
									nextForNode.addParent(continueNode);
									continueNode.addChild(nextForNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
						}else{
							CFGNode nextForNode = new CFGNode();
							IASTBinaryExpression iabe = (IASTBinaryExpression)forCompoundChildren[i].getChildren()[1];
							int offsetTmp = iabe.getFileLocation().getNodeOffset();
							nextForNode.setBinaryExpression(iabe);
							nextForNode.setOffset(offsetTmp);
							nextForNode.setSign(2);
							Builder.nodeNumber++;
							nextForNode.setNodeNumber(Builder.nodeNumber);
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastForStatementOffset){
									Builder.terminalNodes.get(k).addChild(nextForNode);
									nextForNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							Builder.terminalNodes.add(nextForNode);							
							addNode(nextForNode, (IASTForStatement)forCompoundChildren[i]);
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
									nextForNode.addParent(continueNode);
									continueNode.addChild(nextForNode);
								}
							}
							Builder.continueNodes = new ArrayList<CFGNode>();
						}
						firstSequenceStatementIndex = i+1;
					}
				}else if(forCompoundChildren[i] instanceof IASTIfStatement){
					countI++;					
					endStatementIndex = i;
					
					if(countI == 1){
						lastForStatementOffset = forCompoundChildren[i].getFileLocation().getNodeOffset();

						if(i==0){
							Builder.currentNode = new CFGNode();
							parent.addChild(Builder.currentNode);
							Builder.currentNode.addParent(parent);
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							forCompoundChildren[i].accept(ifStatementVisitor);
						}else{
							Builder.currentNode = new CFGNode();
							forNode.addChild(Builder.currentNode);
							Builder.currentNode.addParent(forNode);
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							forCompoundChildren[i].accept(ifStatementVisitor);
						}
						firstSequenceStatementIndex = i+1;
					}else{	// 当前for分支内有2个或2个以上的for语句或者if-else语句
						if(firstSequenceStatementIndex != i){
							CFGNode nextNode = new CFGNode();
							int offsetTmp = forCompoundChildren[firstSequenceStatementIndex].getFileLocation().getNodeOffset();
							nextNode.setSign(-1);
							nextNode.setOffset(offsetTmp);
							Builder.nodeNumber++;
							nextNode.setNodeNumber(Builder.nodeNumber);
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastForStatementOffset){
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
							forCompoundChildren[i].accept(ifStatementVisitor);
						}else{
							Builder.currentNode = new CFGNode();
							int terminalNodesNum = Builder.terminalNodes.size();
							for(int k=(terminalNodesNum-1); k>-1; k--){
								if(Builder.terminalNodes.get(k).getOffset() >= lastForStatementOffset){
									Builder.terminalNodes.get(k).addChild(Builder.currentNode);
									Builder.currentNode.addParent(Builder.terminalNodes.get(k));
									Builder.terminalNodes.remove(k);
								}
							}
							
							IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
							ifStatementVisitor.shouldVisitStatements = true;
							forCompoundChildren[i].accept(ifStatementVisitor);
						}
						firstSequenceStatementIndex = i+1;
					}
				}
			}
			if(countI == 0){
				forNode.addChild(parent);
				parent.addParent(forNode);
			}
			//当前块中最后的语句是顺序语句
			if(endStatementIndex > -1 && endStatementIndex < forCompoundChildrenN-1){
				CFGNode nextNode = new CFGNode();
				int offsetTmp = forCompoundChildren[endStatementIndex+1].getFileLocation().getNodeOffset();
				nextNode.setOffset(offsetTmp);
				nextNode.setSign(-1);
				Builder.nodeNumber++;
				nextNode.setNodeNumber(Builder.nodeNumber);
				
				//为当前顺序节点添加父节点
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastForStatementOffset){
						Builder.terminalNodes.get(k).addChild(nextNode);
						nextNode.addParent(Builder.terminalNodes.get(k));
						Builder.terminalNodes.remove(k);
					}
				}
				
				nextNode.addChild(parent);
				parent.addParent(nextNode);
			}
			if(forCompoundChildren[forCompoundChildrenN-1] instanceof IASTForStatement || forCompoundChildren[forCompoundChildrenN-1] instanceof IASTIfStatement){
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastForStatementOffset){
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
			 * for循环主体是单条语句	 *
			 ************************/
			if(iafs.getChildren()[3] instanceof IASTForStatement){
				IASTBinaryExpression iabe = (IASTBinaryExpression)iafs.getChildren()[3].getChildren()[1];
				int offsetTmp = iabe.getFileLocation().getNodeOffset();
				forNode.setBinaryExpression(iabe);
				forNode.setOffset(offsetTmp);
				forNode.setSign(2);
				Builder.nodeNumber++;
				forNode.setNodeNumber(Builder.nodeNumber);
				parent.addChild(forNode);
				forNode.addParent(parent);
				parent.addParent(forNode);
				forNode.addChild(parent);
				
				addNode(forNode, (IASTForStatement)iafs.getChildren()[3]);
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
						forNode.addParent(continueNode);
						continueNode.addChild(forNode);
					}
				}
				Builder.continueNodes = new ArrayList<CFGNode>();
			}else if(iafs.getChildren()[3] instanceof IASTIfStatement){
				lastForStatementOffset = iafs.getChildren()[3].getFileLocation().getNodeOffset();
				Builder.currentNode = new CFGNode();
				parent.addChild(Builder.currentNode);
				Builder.currentNode.addParent(parent);
				
				IfStatementVisitor ifStatementVisitor = new IfStatementVisitor();
				ifStatementVisitor.shouldVisitStatements = true;
				iafs.getChildren()[3].accept(ifStatementVisitor);
				
				int terminalNodesNum = Builder.terminalNodes.size();
				for(int k=(terminalNodesNum-1); k>-1; k--){
					if(Builder.terminalNodes.get(k).getOffset() >= lastForStatementOffset){
						Builder.terminalNodes.get(k).addChild(parent);
						parent.addParent(Builder.terminalNodes.get(k));
						Builder.terminalNodes.remove(k);
					}
				}
			}else{
				int offsetTmp = iafs.getChildren()[3].getFileLocation().getNodeOffset();
				forNode.setOffset(offsetTmp);
				forNode.setSign(-1);
				Builder.nodeNumber++;
				forNode.setNodeNumber(Builder.nodeNumber);
				parent.addChild(forNode);
				forNode.addParent(parent);
				parent.addParent(forNode);
				forNode.addChild(parent);
			}
		}		
	}
}
