/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.metrics.impl.internal;

import net.sourceforge.pmd.lang.java.ast.ASTFinallyStatement;
import net.sourceforge.pmd.lang.java.ast.ASTLambdaExpression;
import org.apache.commons.lang3.mutable.MutableInt;

import net.sourceforge.pmd.lang.java.ast.ASTAssertStatement;
import net.sourceforge.pmd.lang.java.ast.ASTBlockStatement;
import net.sourceforge.pmd.lang.java.ast.ASTCatchStatement;
import net.sourceforge.pmd.lang.java.ast.ASTConditionalExpression;
import net.sourceforge.pmd.lang.java.ast.ASTDoStatement;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTForStatement;
import net.sourceforge.pmd.lang.java.ast.ASTIfStatement;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchExpression;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchLabel;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchLabeledRule;
import net.sourceforge.pmd.lang.java.ast.ASTSwitchStatement;
import net.sourceforge.pmd.lang.java.ast.ASTThrowStatement;
import net.sourceforge.pmd.lang.java.ast.ASTWhileStatement;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.ast.JavaParserVisitorAdapter;
import net.sourceforge.pmd.lang.java.metrics.impl.CycloMetric;
import net.sourceforge.pmd.lang.java.metrics.impl.CycloMetric.CycloOption;
import net.sourceforge.pmd.lang.metrics.MetricOptions;


/**
 * Visitor for the Cyclo metric.
 *
 * @author Clément Fournier
 * @since 6.7.0
 */
public class CycloVisitor extends JavaParserVisitorAdapter {


    protected final boolean considerBooleanPaths;
    protected final boolean considerAssert;
    private final JavaNode topNode;


    public CycloVisitor(MetricOptions options, JavaNode topNode) {
        considerBooleanPaths = !options.getOptions().contains(CycloOption.IGNORE_BOOLEAN_PATHS);
        considerAssert = options.getOptions().contains(CycloOption.CONSIDER_ASSERT);
        this.topNode = topNode;
    }


    @Override
    public final Object visit(JavaNode localNode, Object data) {
        return localNode.isFindBoundary() && !localNode.equals(topNode) ? data : super.visit(localNode, data);
    }

    @Override
    public Object visit(ASTSwitchExpression node, Object data) {
        return handleSwitch(node, (MutableInt) data);
    }

    @Override
    public Object visit(ASTSwitchStatement node, Object data) {
        return handleSwitch(node, (MutableInt) data);
    }

    private Object handleSwitch(JavaNode node, MutableInt data) {

        if (considerBooleanPaths) {
            data.add(CycloMetric.booleanExpressionComplexity(node.getChild(0)));
        }

        for (ASTSwitchLabel label : node.findChildrenOfType(ASTSwitchLabel.class)) {
            if (label.isDefault()) {
                // like for "else", default is not a decision point
                continue;
            }

            if (considerBooleanPaths) {
                data.add(label.findChildrenOfType(ASTExpression.class).size());
            } else if (node.getNumChildren() > 1 + label.getIndexInParent()
                    && node.getChild(label.getIndexInParent() + 1) instanceof ASTBlockStatement) {
                // an empty label is only counted if we count boolean paths
                data.increment();
            }
        }

        for (ASTSwitchLabeledRule rule : node.findChildrenOfType(ASTSwitchLabeledRule.class)) {
            ASTSwitchLabel label = rule.getFirstChildOfType(ASTSwitchLabel.class);
            if (label.isDefault()) {
                continue;
            }
            if (considerBooleanPaths) {
                data.add(label.findChildrenOfType(ASTExpression.class).size());
            }
        }

        return visit(node, data);
    }


    @Override
    public Object visit(ASTConditionalExpression node, Object data) {
        ((MutableInt) data).increment();
        if (considerBooleanPaths) {
            ((MutableInt) data).add(CycloMetric.booleanExpressionComplexity(node.getCondition()));
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTLambdaExpression node, Object data) {
        ((MutableInt) data).increment();
        return super.visit(node, data);
    }



    @Override
    public Object visit(ASTWhileStatement node, Object data) {
        ((MutableInt) data).increment();
        if (considerBooleanPaths) {
            ((MutableInt) data).add(CycloMetric.booleanExpressionComplexity(node.getCondition()));
        }
        return super.visit(node, data);
    }


    @Override
    public Object visit(ASTIfStatement node, Object data) {
        ((MutableInt) data).increment();
        if (node.hasElse()) {
            ((MutableInt) data).increment();
        }
        if (considerBooleanPaths) {
            ((MutableInt) data).add(CycloMetric.booleanExpressionComplexity(node.getCondition()));
        }

        return super.visit(node, data);
    }


    @Override
    public Object visit(ASTForStatement node, Object data) {
        ((MutableInt) data).increment();

        if (considerBooleanPaths && !node.isForeach()) {
            ((MutableInt) data).add(CycloMetric.booleanExpressionComplexity(node.getCondition()));
        }

        return super.visit(node, data);
    }


    @Override
    public Object visit(ASTDoStatement node, Object data) {
        ((MutableInt) data).increment();
        if (considerBooleanPaths) {
            ((MutableInt) data).add(CycloMetric.booleanExpressionComplexity(node.getCondition()));
        }

        return super.visit(node, data);
    }


    @Override
    public Object visit(ASTCatchStatement node, Object data) {
        ((MutableInt) data).increment();
        return super.visit(node, data);
    }


    @Override
    public Object visit(ASTThrowStatement node, Object data) {
        ((MutableInt) data).increment();
        return super.visit(node, data);
    }



    @Override
    public Object visit(ASTFinallyStatement node, Object data) {
        ((MutableInt) data).increment();
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTAssertStatement node, Object data) {
        if (considerAssert) {
            ((MutableInt) data).add(2); // equivalent to if (condition) { throw .. }

            if (considerBooleanPaths) {
                ((MutableInt) data).add(CycloMetric.booleanExpressionComplexity(node.getCondition()));
            }
        }

        return super.visit(node, data);
    }

}
