package com.wakwau.xplore.treeview.state

import com.wakwau.xplore.treeview.model.TreeNode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TreeStateTest {

    private lateinit var treeState: TreeState<String>

    @Before
    fun setup() {
        treeState = TreeState()
    }

    @Test
    fun testEmptyTree() {
        assertTrue(treeState.roots.isEmpty())
        assertTrue(treeState.visibleNodes.value.isEmpty())
    }

    @Test
    fun testSingleRoot() {
        val root = TreeNode("Root")
        treeState.setRoots(listOf(root))
        assertEquals(1, treeState.visibleNodes.value.size)
        assertEquals("Root", treeState.visibleNodes.value[0].node.data)
        assertEquals(0, treeState.visibleNodes.value[0].depth)
    }

    @Test
    fun testMultipleRoots() {
        treeState.setRoots(listOf(TreeNode("Root1"), TreeNode("Root2")))
        assertEquals(2, treeState.visibleNodes.value.size)
    }

    @Test
    fun testCollapsedRoot() {
        val root = TreeNode("Root")
        root.addChild(TreeNode("Child"))
        treeState.setRoots(listOf(root))
        // Collapsed by default, child should not be visible
        assertEquals(1, treeState.visibleNodes.value.size)
    }

    @Test
    fun testExpandedRoot() {
        val root = TreeNode("Root")
        root.addChild(TreeNode("Child"))
        root.expand()
        treeState.setRoots(listOf(root))
        assertEquals(2, treeState.visibleNodes.value.size)
        assertEquals("Child", treeState.visibleNodes.value[1].node.data)
    }

    @Test
    fun testNestedChild() {
        val root = TreeNode("Root")
        val child = TreeNode("Child")
        root.addChild(child)
        root.expand()
        treeState.setRoots(listOf(root))
        assertEquals(1, treeState.visibleNodes.value[1].depth)
    }

    @Test
    fun testNestedGrandchild() {
        val root = TreeNode("Root")
        val child = TreeNode("Child")
        val grandchild = TreeNode("Grandchild")
        root.addChild(child)
        child.addChild(grandchild)
        
        root.expand()
        child.expand()
        treeState.setRoots(listOf(root))
        
        val visible = treeState.visibleNodes.value
        assertEquals(3, visible.size)
        assertEquals(2, visible[2].depth)
        assertEquals("Grandchild", visible[2].node.data)
    }

    @Test
    fun testCollapseParentHidesDescendants() {
        val root = TreeNode("Root")
        val child = TreeNode("Child")
        val grandchild = TreeNode("Grandchild")
        root.addChild(child)
        child.addChild(grandchild)
        
        root.expand()
        child.expand()
        treeState.setRoots(listOf(root))
        assertEquals(3, treeState.visibleNodes.value.size)
        
        treeState.collapse(root)
        assertEquals(1, treeState.visibleNodes.value.size)
    }

    // [Jalur Class/Modul]: treeview/src/test/java/com/wakwau/xplore/treeview/state/TreeStateTest.kt
    // [Penjelasan]: Memverifikasi bahwa collapseRecursively menutup node beserta seluruh descendantnya sehingga saat root di-expand ulang, descendant tetap tertutup.
    @Test
    fun testCollapseRecursively() {
        val root = TreeNode("Root")
        val child = TreeNode("Child")
        val grandchild = TreeNode("Grandchild")
        root.addChild(child)
        child.addChild(grandchild)

        root.expand()
        child.expand()
        grandchild.expand()
        treeState.setRoots(listOf(root))
        assertEquals(3, treeState.visibleNodes.value.size)

        treeState.collapseRecursively(child)
        assertFalse(child.isExpanded)
        assertFalse(grandchild.isExpanded)
        assertEquals(2, treeState.visibleNodes.value.size)
    }

    @Test
    fun testExpandParentRestoresDescendants() {
        val root = TreeNode("Root")
        val child = TreeNode("Child")
        val grandchild = TreeNode("Grandchild")
        root.addChild(child)
        child.addChild(grandchild)
        
        child.expand() // Child expanded, but root collapsed
        treeState.setRoots(listOf(root))
        assertEquals(1, treeState.visibleNodes.value.size)
        
        treeState.expand(root)
        // Root expanded, its child is also expanded, so grandchild is visible
        assertEquals(3, treeState.visibleNodes.value.size)
    }

    @Test
    fun testDepthCorrectness() {
        val root = TreeNode("Root")
        val child = TreeNode("Child")
        val grandchild = TreeNode("Grandchild")
        root.addChild(child)
        child.addChild(grandchild)
        root.expand()
        child.expand()
        treeState.setRoots(listOf(root))
        
        val visible = treeState.visibleNodes.value
        assertEquals(0, visible[0].depth)
        assertEquals(1, visible[1].depth)
        assertEquals(2, visible[2].depth)
    }

    @Test
    fun testStableOrdering() {
        val root = TreeNode("Root")
        root.addChild(TreeNode("Child1"))
        root.addChild(TreeNode("Child2"))
        root.expand()
        treeState.setRoots(listOf(root))
        
        val visible = treeState.visibleNodes.value
        assertEquals("Child1", visible[1].node.data)
        assertEquals("Child2", visible[2].node.data)
    }

    @Test
    fun testMultipleBranches() {
        val root1 = TreeNode("Root1")
        root1.addChild(TreeNode("Child1.1"))
        root1.expand()
        
        val root2 = TreeNode("Root2")
        root2.addChild(TreeNode("Child2.1"))
        // root2 is collapsed
        
        treeState.setRoots(listOf(root1, root2))
        val visible = treeState.visibleNodes.value
        
        assertEquals(3, visible.size)
        assertEquals("Root1", visible[0].node.data)
        assertEquals("Child1.1", visible[1].node.data)
        assertEquals("Root2", visible[2].node.data)
    }

    @Test
    fun testDeepHierarchy() {
        var current = TreeNode("Node0")
        val root = current
        for (i in 1..10) {
            val child = TreeNode("Node$i")
            current.addChild(child)
            current.expand()
            current = child
        }
        treeState.setRoots(listOf(root))
        assertEquals(11, treeState.visibleNodes.value.size)
        assertEquals(10, treeState.visibleNodes.value[10].depth)
    }

    @Test
    fun testSetRoots() {
        treeState.setRoots(listOf(TreeNode("A")))
        assertEquals(1, treeState.visibleNodes.value.size)
        treeState.setRoots(listOf(TreeNode("B"), TreeNode("C")))
        assertEquals(2, treeState.visibleNodes.value.size)
    }

    @Test
    fun testClear() {
        treeState.setRoots(listOf(TreeNode("A")))
        treeState.clear()
        assertTrue(treeState.roots.isEmpty())
        assertTrue(treeState.visibleNodes.value.isEmpty())
    }
}
