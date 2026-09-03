package com.wakwau.xplore.treeview.model

import com.wakwau.xplore.treeview.state.TreeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TreeScopeCalculatorTest {

    private lateinit var treeState: TreeState<String>

    @Before
    fun setup() {
        treeState = TreeState()
    }

    @Test
    fun `test null or non-matching focus returns null`() {
        val root = TreeNode("Root", id = "root_id")
        treeState.setRoots(listOf(root))

        val visible = treeState.visibleNodes.value

        assertNull(TreeScopeCalculator.calculateFocusRange(visible, null))
        assertNull(TreeScopeCalculator.calculateFocusRange(visible, "non_existent"))
        assertEquals(BorderPosition.NONE, TreeScopeCalculator.getBorderPosition(0, null))
    }

    @Test
    fun `test single leaf node focus returns SINGLE`() {
        val fileNode = TreeNode("File1.txt", id = "file1_id")
        treeState.setRoots(listOf(fileNode))

        val visible = treeState.visibleNodes.value
        val range = TreeScopeCalculator.calculateFocusRange(visible, "file1_id")

        assertEquals(0..0, range)
        assertEquals(BorderPosition.SINGLE, TreeScopeCalculator.getBorderPosition(0, range))
        assertEquals(BorderPosition.NONE, TreeScopeCalculator.getBorderPosition(1, range))
    }

    @Test
    fun `test collapsed directory node focus returns SINGLE`() {
        val root = TreeNode("Folder", id = "folder_id")
        val child = TreeNode("Child.txt", id = "child_id")
        root.addChild(child)
        // Root is collapsed
        treeState.setRoots(listOf(root))

        val visible = treeState.visibleNodes.value
        val range = TreeScopeCalculator.calculateFocusRange(visible, "folder_id")

        assertEquals(0..0, range)
        assertEquals(BorderPosition.SINGLE, TreeScopeCalculator.getBorderPosition(0, range))
    }

    @Test
    fun `test expanded directory with children returns TOP MIDDLE BOTTOM`() {
        val root = TreeNode("Folder", id = "folder_id")
        val child1 = TreeNode("Child1.txt", id = "c1")
        val child2 = TreeNode("Child2.txt", id = "c2")
        val child3 = TreeNode("Child3.txt", id = "c3")
        root.addChild(child1)
        root.addChild(child2)
        root.addChild(child3)
        root.expand()

        treeState.setRoots(listOf(root))
        val visible = treeState.visibleNodes.value

        val range = TreeScopeCalculator.calculateFocusRange(visible, "folder_id")
        assertEquals(0..3, range)

        assertEquals(BorderPosition.TOP, TreeScopeCalculator.getBorderPosition(0, range))
        assertEquals(BorderPosition.MIDDLE, TreeScopeCalculator.getBorderPosition(1, range))
        assertEquals(BorderPosition.MIDDLE, TreeScopeCalculator.getBorderPosition(2, range))
        assertEquals(BorderPosition.BOTTOM, TreeScopeCalculator.getBorderPosition(3, range))
    }

    @Test
    fun `test expanded directory with nested subtree focus range covers all descendants`() {
        val root = TreeNode("Root", id = "root_id")
        val subFolder = TreeNode("SubFolder", id = "sub_id")
        val subChild = TreeNode("DeepChild.txt", id = "deep_id")
        val fileAtRoot = TreeNode("FileRoot.txt", id = "file_root_id")

        subFolder.addChild(subChild)
        root.addChild(subFolder)
        root.addChild(fileAtRoot)

        root.expand()
        subFolder.expand()
        treeState.setRoots(listOf(root))

        val visible = treeState.visibleNodes.value
        // visible: [0: Root, 1: SubFolder, 2: DeepChild, 3: FileRoot]
        assertEquals(4, visible.size)

        // Focus on Root: covers 0..3
        val rootRange = TreeScopeCalculator.calculateFocusRange(visible, "root_id")
        assertEquals(0..3, rootRange)
        assertEquals(BorderPosition.TOP, TreeScopeCalculator.getBorderPosition(0, rootRange))
        assertEquals(BorderPosition.MIDDLE, TreeScopeCalculator.getBorderPosition(1, rootRange))
        assertEquals(BorderPosition.MIDDLE, TreeScopeCalculator.getBorderPosition(2, rootRange))
        assertEquals(BorderPosition.BOTTOM, TreeScopeCalculator.getBorderPosition(3, rootRange))

        // Focus on SubFolder: covers 1..2
        val subRange = TreeScopeCalculator.calculateFocusRange(visible, "sub_id")
        assertEquals(1..2, subRange)
        assertEquals(BorderPosition.NONE, TreeScopeCalculator.getBorderPosition(0, subRange))
        assertEquals(BorderPosition.TOP, TreeScopeCalculator.getBorderPosition(1, subRange))
        assertEquals(BorderPosition.BOTTOM, TreeScopeCalculator.getBorderPosition(2, subRange))
        assertEquals(BorderPosition.NONE, TreeScopeCalculator.getBorderPosition(3, subRange))
    }

    @Test
    fun `test expanded directory with 1 child has TOP and BOTTOM without MIDDLE`() {
        val root = TreeNode("Folder", id = "f_id")
        val child = TreeNode("OnlyChild.txt", id = "c_id")
        root.addChild(child)
        root.expand()

        treeState.setRoots(listOf(root))
        val visible = treeState.visibleNodes.value

        val range = TreeScopeCalculator.calculateFocusRange(visible, "f_id")
        assertEquals(0..1, range)
        assertEquals(BorderPosition.TOP, TreeScopeCalculator.getBorderPosition(0, range))
        assertEquals(BorderPosition.BOTTOM, TreeScopeCalculator.getBorderPosition(1, range))
    }
}
