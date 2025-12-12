#!/usr/bin/env python3
"""
BPMN Visual Representation Generator
Generates BPMNShape and BPMNEdge elements for BPMN diagrams

Follows dimension standards from PROMPT_TÉCNICO_BPMN.md:
- Start Event: 36x36
- End Event: 36x36
- Service Task: 100x80
- User Task: 100x80
- Call Activity: 130x90
- Gateway: 50x50
- SubProcess: 350x300 (expandable)
"""

import xml.etree.ElementTree as ET
from typing import Dict, List, Tuple
import sys

# Namespaces
NAMESPACES = {
    'bpmn': 'http://www.omg.org/spec/BPMN/20100524/MODEL',
    'bpmndi': 'http://www.omg.org/spec/BPMN/20100524/DI',
    'dc': 'http://www.omg.org/spec/DD/20100524/DC',
    'di': 'http://www.omg.org/spec/DD/20100524/DI',
    'camunda': 'http://camunda.org/schema/1.0/bpmn',
    'xsi': 'http://www.w3.org/2001/XMLSchema-instance'
}

# Dimension standards
DIMENSIONS = {
    'startEvent': {'width': 36, 'height': 36},
    'endEvent': {'width': 36, 'height': 36},
    'intermediateThrowEvent': {'width': 36, 'height': 36},
    'intermediateCatchEvent': {'width': 36, 'height': 36},
    'boundaryEvent': {'width': 36, 'height': 36},
    'serviceTask': {'width': 100, 'height': 80},
    'userTask': {'width': 100, 'height': 80},
    'sendTask': {'width': 100, 'height': 80},
    'receiveTask': {'width': 100, 'height': 80},
    'businessRuleTask': {'width': 100, 'height': 80},
    'callActivity': {'width': 130, 'height': 90},
    'exclusiveGateway': {'width': 50, 'height': 50},
    'parallelGateway': {'width': 50, 'height': 50},
    'inclusiveGateway': {'width': 50, 'height': 50},
    'subProcess': {'width': 350, 'height': 300, 'isExpanded': True},
    'task': {'width': 100, 'height': 80}
}

# Layout configuration
LAYOUT_CONFIG = {
    'start_x': 160,
    'start_y': 100,
    'horizontal_spacing': 150,
    'vertical_spacing': 100,
    'lane_height': 400,
    'subprocess_indent': 50
}


class BPMNVisualGenerator:
    def __init__(self, bpmn_file_path: str):
        self.file_path = bpmn_file_path
        self.tree = ET.parse(bpmn_file_path)
        self.root = self.tree.getroot()

        # Register namespaces
        for prefix, uri in NAMESPACES.items():
            ET.register_namespace(prefix, uri)

        self.shapes_generated = 0
        self.edges_generated = 0

    def get_element_type(self, element) -> str:
        """Determine BPMN element type from XML tag"""
        tag = element.tag
        for ns_prefix in NAMESPACES.values():
            tag = tag.replace(f'{{{ns_prefix}}}', '')
        return tag

    def get_dimensions(self, element_type: str) -> Dict[str, int]:
        """Get width and height for element type"""
        return DIMENSIONS.get(element_type, {'width': 100, 'height': 80})

    def calculate_layout_position(self, index: int, level: int = 0, is_subprocess: bool = False) -> Tuple[int, int]:
        """Calculate x, y position for element in flow"""
        x = LAYOUT_CONFIG['start_x'] + (index * LAYOUT_CONFIG['horizontal_spacing'])
        y = LAYOUT_CONFIG['start_y'] + (level * LAYOUT_CONFIG['vertical_spacing'])

        if is_subprocess:
            x += LAYOUT_CONFIG['subprocess_indent']
            y += LAYOUT_CONFIG['subprocess_indent']

        return x, y

    def create_shape_element(self, element_id: str, element_type: str, x: int, y: int,
                             is_expanded: bool = None) -> ET.Element:
        """Create BPMNShape element"""
        shape = ET.Element(f'{{{NAMESPACES["bpmndi"]}}}BPMNShape')
        shape.set('id', f'{element_id}_di')
        shape.set('bpmnElement', element_id)

        # Add isExpanded attribute for subProcesses
        if is_expanded is not None:
            shape.set('isExpanded', str(is_expanded).lower())

        # Create Bounds
        bounds = ET.SubElement(shape, f'{{{NAMESPACES["dc"]}}}Bounds')
        dims = self.get_dimensions(element_type)
        bounds.set('x', str(x))
        bounds.set('y', str(y))
        bounds.set('width', str(dims['width']))
        bounds.set('height', str(dims['height']))

        self.shapes_generated += 1
        return shape

    def create_edge_element(self, flow_id: str, waypoints: List[Tuple[int, int]]) -> ET.Element:
        """Create BPMNEdge element with waypoints"""
        edge = ET.Element(f'{{{NAMESPACES["bpmndi"]}}}BPMNEdge')
        edge.set('id', f'{flow_id}_di')
        edge.set('bpmnElement', flow_id)

        # Add waypoints
        for x, y in waypoints:
            waypoint = ET.SubElement(edge, f'{{{NAMESPACES["di"]}}}waypoint')
            waypoint.set('x', str(x))
            waypoint.set('y', str(y))

        self.edges_generated += 1
        return edge

    def find_diagram_plane(self):
        """Find or create BPMNDiagram and BPMNPlane"""
        # Find BPMNDiagram
        diagram = self.root.find('.//bpmndi:BPMNDiagram', NAMESPACES)

        if diagram is None:
            # Create BPMNDiagram
            diagram = ET.Element(f'{{{NAMESPACES["bpmndi"]}}}BPMNDiagram')
            diagram.set('id', 'BPMNDiagram_1')
            self.root.append(diagram)

        # Find BPMNPlane
        plane = diagram.find('.//bpmndi:BPMNPlane', NAMESPACES)

        if plane is None:
            # Get process ID
            process = self.root.find('.//bpmn:process', NAMESPACES)
            process_id = process.get('id') if process is not None else 'Process_1'

            plane = ET.SubElement(diagram, f'{{{NAMESPACES["bpmndi"]}}}BPMNPlane')
            plane.set('id', 'BPMNPlane_1')
            plane.set('bpmnElement', process_id)

        return plane

    def generate_visuals_for_process(self):
        """Generate visual representations for all process elements"""
        plane = self.find_diagram_plane()

        # Find the main process
        process = self.root.find('.//bpmn:process', NAMESPACES)
        if process is None:
            print("ERROR: No process found in BPMN file")
            return

        print(f"Processing: {process.get('id')} - {process.get('name')}")

        # Generate shapes and edges
        index = 0
        flow_connections = []

        # Process all direct children of process
        for child in process:
            element_type = self.get_element_type(child)
            element_id = child.get('id')

            if not element_id:
                continue

            # Skip sequence flows (process later)
            if element_type == 'sequenceFlow':
                source = child.get('sourceRef')
                target = child.get('targetRef')
                flow_connections.append((element_id, source, target))
                continue

            # Calculate position
            x, y = self.calculate_layout_position(index, 0)

            # Check if subprocess
            is_expanded = None
            if element_type == 'subProcess':
                is_expanded = True

            # Create shape
            shape = self.create_shape_element(element_id, element_type, x, y, is_expanded)
            plane.append(shape)

            print(f"  + Shape: {element_id} ({element_type}) at ({x}, {y})")

            index += 1

        # Generate edges for sequence flows
        for flow_id, source_id, target_id in flow_connections:
            # Simple waypoints (start -> end)
            # In production, calculate actual connector points
            waypoints = [(100, 100), (200, 100)]
            edge = self.create_edge_element(flow_id, waypoints)
            plane.append(edge)
            print(f"  + Edge: {flow_id} ({source_id} -> {target_id})")

    def save(self, output_path: str = None):
        """Save modified BPMN file"""
        if output_path is None:
            output_path = self.file_path

        # Write with proper formatting
        ET.indent(self.tree, space='  ')
        self.tree.write(output_path, encoding='UTF-8', xml_declaration=True)

        print(f"\n✅ Saved: {output_path}")
        print(f"   Shapes generated: {self.shapes_generated}")
        print(f"   Edges generated: {self.edges_generated}")


def main():
    if len(sys.argv) < 2:
        print("Usage: python bpmn_visual_generator.py <bpmn_file>")
        sys.exit(1)

    bpmn_file = sys.argv[1]

    print(f"BPMN Visual Generator")
    print(f"{'='*60}")
    print(f"Input: {bpmn_file}\n")

    generator = BPMNVisualGenerator(bpmn_file)
    generator.generate_visuals_for_process()
    generator.save()

    print(f"\n{'='*60}")
    print(f"✅ Visual generation complete!")


if __name__ == '__main__':
    main()
