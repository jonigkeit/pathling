/*
 * Copyright © Australian e-Health Research Centre, CSIRO. All rights reserved.
 */

import React, { useState } from 'react'
import { MenuItem, Menu, ContextMenu } from '@blueprintjs/core'

import store from '../../store'
import { addAggregation, addGrouping } from '../../store/Actions'

import {
  resourceTree,
  complexTypesTree,
  supportedComplexTypes,
} from '../../fhir/ResourceTree'
import ResourceTreeNode from '../ResourceTreeNode'

function ElementTreeNode(props) {
  const {
      name,
      path,
      treePath,
      type,
      resourceOrComplexType,
      referenceTypes,
    } = props,
    [isExpanded, setExpanded] = useState(false),
    isInResourceTree = !!resourceTree.get(resourceOrComplexType),
    backboneElementChildren = isInResourceTree
      ? resourceTree.getIn(treePath).get('children')
      : null,
    isComplexType = supportedComplexTypes.includes(type),
    complexElementChildren = isComplexType ? complexTypesTree.get(type) : null,
    isReference = type === 'Reference',
    referenceChildren = isReference
      ? referenceTypes.filter(t => !!resourceTree.get(t))
      : null

  /**
   * Opens a context menu at the supplied mouse event which provides actions for
   * adding the specified node to the current query.
   */
  function openContextMenu(event) {
    const aggregationExpression = `${path}.count()`,
      aggregationLabel = aggregationExpression,
      groupingExpression = path,
      groupingLabel = groupingExpression,
      aggregationMenuItem = (
        <MenuItem
          icon="trending-up"
          text={`Add "${aggregationExpression}" to aggregations`}
          onClick={() =>
            store.dispatch(
              addAggregation({
                expression: aggregationExpression,
                label: aggregationLabel,
              }),
            )
          }
        />
      ),
      groupingMenuItem = (
        <MenuItem
          icon="graph"
          text={`Add "${groupingExpression}" to groupings`}
          onClick={() =>
            store.dispatch(
              addGrouping({
                expression: groupingExpression,
                label: groupingLabel,
              }),
            )
          }
        />
      )
    ContextMenu.show(
      <Menu>
        {aggregationMenuItem}
        {groupingMenuItem}
      </Menu>,
      { left: event.clientX, top: event.clientY },
    )
  }

  function renderBackboneElementChildren() {
    const elementTreeNodes = backboneElementChildren.map((node, i) => (
      <ElementTreeNode
        {...node.delete('children').toJS()}
        key={i}
        treePath={treePath.concat('children', i)}
        resourceOrComplexType={resourceOrComplexType}
      />
    ))
    return (
      <ol className="child-nodes bp3-tree-node-list">{elementTreeNodes}</ol>
    )
  }

  function renderComplexElementChildren() {
    const elementTreeNodes = complexElementChildren.map((node, i) => (
      <ElementTreeNode
        {...node.delete('children').toJS()}
        key={i}
        path={`${path}.${node.get('name')}`}
        treePath={[type, i]}
        resourceOrComplexType={isComplexType ? type : resourceOrComplexType}
      />
    ))
    return (
      <ol className="child-nodes bp3-tree-node-list">{elementTreeNodes}</ol>
    )
  }

  function renderReferenceChildren() {
    const resourceTreeNodes = referenceChildren.map((type, i) => (
      <ResourceTreeNode name={type} key={i} />
    ))
    return (
      <ol className="child-nodes bp3-tree-node-list">{resourceTreeNodes}</ol>
    )
  }

  function renderActionIcon() {
    return isComplexType || isReference ? null : (
      <span
        className="bp3-tree-node-secondary-label bp3-icon-standard bp3-icon-arrow-right"
        onClick={openContextMenu}
      />
    )
  }

  function getCaretClasses() {
    if (
      backboneElementChildren ||
      complexElementChildren ||
      (referenceChildren && referenceChildren.length > 0)
    ) {
      return isExpanded
        ? 'bp3-tree-node-caret bp3-tree-node-caret-open bp3-icon-standard'
        : 'bp3-tree-node-caret bp3-tree-node-caret-close bp3-icon-standard'
    } else {
      return 'bp3-tree-node-caret-none bp3-icon-standard'
    }
  }

  function getIconClasses() {
    let iconName = null
    if (isComplexType) {
      iconName = 'grid-view'
    } else if (isReference) {
      iconName = 'document-share'
    } else if (backboneElementChildren) {
      iconName = 'folder-close'
    } else {
      iconName = 'symbol-square'
    }
    return `bp3-tree-node-icon bp3-icon-standard bp3-icon-${iconName}`
  }

  function getNameClasses() {
    return isComplexType || isReference
      ? 'name bp3-tree-node-label'
      : 'name clickable bp3-tree-node-label'
  }

  return (
    <li className="element-tree-node bp3-tree-node">
      <div className="bp3-tree-node-content">
        <span
          className={getCaretClasses()}
          onClick={() => setExpanded(!isExpanded)}
        />
        <span className={getIconClasses()} />
        <span className={getNameClasses()}>{name}</span>
        {renderActionIcon()}
      </div>
      {isExpanded && backboneElementChildren
        ? renderBackboneElementChildren()
        : null}
      {isExpanded && complexElementChildren
        ? renderComplexElementChildren()
        : null}
      {isExpanded && referenceChildren ? renderReferenceChildren() : null}
    </li>
  )
}

export default ElementTreeNode