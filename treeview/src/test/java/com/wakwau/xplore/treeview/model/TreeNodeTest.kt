package com.wakwau.xplore.treeview.model

import org.junit.Assert.*
import org.junit.Test

class TreeNodeTest {

    @Test
    fun testRootCreation() {
        val root = TreeNode("Root")
        assertTrue(root.isRoot)
        assertEquals(0, root.depth)
        assertNull(root.parent)
        assertFalse(root.hasChildren)
        assertEquals("Root", root.data)
    }

    @Test
    fun testChildRelationship() {
        val root = TreeNode("Root")
        val child = TreeNode("Child")
        
        root.addChild(child)
        
        assertFalse(child.isRoot)
        assertTrue(root.hasChildren)
        assertEquals(root, child.parent)
        assertEquals(1, child.depth)
        assertEquals(1, root.children.size)
        assertEquals(child, root.children[0])
    }

    @Test
    fun testParentRelationship() {
        val parent = TreeNode("Parent")
        val child = TreeNode("Child")
        parent.addChild(child)
        assertEquals(parent, child.parent)
    }

    @Test
    fun testHasChildren() {
        val root = TreeNode("Root")
        assertFalse(root.hasChildren)
        root.addChild(TreeNode("Child"))
        assertTrue(root.hasChildren)
    }

    @Test
    fun testAddChild() {
        val parent = TreeNode("Parent")
        val child = TreeNode("Child")
        parent.addChild(child)
        assertTrue(parent.children.contains(child))
        assertEquals(parent, child.parent)
    }

    @Test
    fun testRemoveChild() {
        val parent = TreeNode("Parent")
        val child = TreeNode("Child")
        parent.addChild(child)
        
        parent.removeChild(child)
        
        assertFalse(parent.hasChildren)
        assertNull(child.parent)
    }

    @Test
    fun testExpand() {
        val node = TreeNode("Node")
        assertFalse(node.isExpanded)
        node.expand()
        assertTrue(node.isExpanded)
    }

    @Test
    fun testCollapse() {
        val node = TreeNode("Node")
        node.expand()
        assertTrue(node.isExpanded)
        node.collapse()
        assertFalse(node.isExpanded)
    }

    @Test
    fun testToggle() {
        val node = TreeNode("Node")
        assertFalse(node.isExpanded)
        node.toggleExpanded()
        assertTrue(node.isExpanded)
        node.toggleExpanded()
        assertFalse(node.isExpanded)
    }

    @Test
    fun testIdentityStabil() {
        val node1 = TreeNode("Node")
        val node2 = TreeNode("Node")
        assertNotEquals(node1.id, node2.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCycleProtection() {
        val root = TreeNode("Root")
        val child = TreeNode("Child")
        val grandchild = TreeNode("Grandchild")
        
        root.addChild(child)
        child.addChild(grandchild)
        
        // This should throw an exception
        grandchild.addChild(root)
    }
}
