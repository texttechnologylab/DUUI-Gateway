/**
 * This file contains constants, types, interfaces and functions related to DUUI components.
 */

import { v4 as uuidv4 } from 'uuid'

export const DUUIRemoteDriver = 'DUUIRemoteDriver'
export const DUUIDockerDriver = 'DUUIDockerDriver'
export const DUUISwarmDriver = 'DUUISwarmDriver'
export const DUUIUIMADriver = 'DUUIUIMADriver'
export const DUUIKubernetesDriver = 'DUUIKubernetesDriver'
export const DUUIPodmanDriver = 'DUUIPodmanDriver'

export type DUUIDriver =
	| 'DUUIRemoteDriver'
	| 'DUUIDockerDriver'
	| 'DUUISwarmDriver'
	| 'DUUIUIMADriver'
	| 'DUUIKubernetesDriver'
	| 'DUUIPodmanDriver'

export type DUUIDriverFilter = DUUIDriver | 'Any'
export const DUUIDriverFilters = [
	'Any',
	'DUUIRemoteDriver',
	'DUUIDockerDriver',
	'DUUISwarmDriver',
	// 'DUUIUIMADriver',
	'DUUIKubernetesDriver',
	'DUUIPodmanDriver'
]

export const RegistryDrivers = [
    DUUIDockerDriver,
    DUUISwarmDriver,
    DUUIPodmanDriver
]

export const LabelDrivers = [
    DUUIDockerDriver,
    DUUISwarmDriver,
    DUUIKubernetesDriver,
    DUUIPodmanDriver
]

// Fully qualified UIMA annotation class name, e.g. "org.example.MyAnnotation".
// Kept as string at runtime / JSON, but branded in TypeScript for clarity.
export type UIMAAnnotationClass = string & { __brand: 'UIMAAnnotationClass' }

// optional language info in each meta entry
export type Language = {
	name: string;
	code: string;
};

export type DUUIComponentMetaData = {
	tag: string;
	search_tags: string[];
	documentation: string;
	description: string;
	short_description: string;
	references: string[];
	language: Language[] | null;
	required_types: string[];
	resulting_types: string[];
};

export type DUUIRegistryEntry = {
	_id: string;
	name: string;
	meta_data: DUUIComponentMetaData[];
	registry_id: string;
	registry_name: string;
	registry_url: string;
};

export type DUUIRegistryEntryList = DUUIRegistryEntry[];


export const DUUIDrivers: string[] = [
	DUUIRemoteDriver,
	DUUIDockerDriver,
	DUUISwarmDriver,
	// DUUIUIMADriver,
	DUUIKubernetesDriver,
    DUUIPodmanDriver
]

export type componentOptions =
	| 'use_GPU'
	| 'docker_image_fetching'
	| 'scale'
	| 'registry_auth'
	| 'constraints'
	| 'labels'
	| 'host'
	| 'ignore_200_error'

export interface DUUIComponent {
	oid: string
	id: string // Used for Drag & Drop
	name: string
	description: string
	tags: string[]
	driver: DUUIDriver
	target: string
	inputs: UIMAAnnotationClass[]
	outputs: UIMAAnnotationClass[]
	options: {
		use_GPU: boolean
		docker_image_fetching: boolean
		scale: number
		keep_alive: boolean
		registry_auth: {
			username: string
			password: string
		}
		constraints: string[]
		labels: string[]
		host: string
		ignore_200_error: boolean
	}
	parameters: any
	pipeline_id: string | null
	user_id: string | null
	index: number
}

export type Dependency = {
  from: string;
  to: string;
  types: string[];
};

export function buildGraph(components: DUUIComponent[]) {
  const typeProducers = new Map<string, Set<string>>();

  for (const c of components) {
    for (const t of c.outputs ?? []) {
      if (!typeProducers.has(t)) typeProducers.set(t, new Set());
      typeProducers.get(t)!.add(c.id);
    }
  }

  const edgeKeyToTypes = new Map<string, Set<string>>();

  for (const c of components) {
    for (const t of c.inputs ?? []) {
      const producers = typeProducers.get(t);
      if (!producers) continue; // pre-annotated input

      for (const fromId of producers) {
        if (fromId === c.id) continue;
        const key = `${fromId}->${c.id}`;
        if (!edgeKeyToTypes.has(key)) edgeKeyToTypes.set(key, new Set());
        edgeKeyToTypes.get(key)!.add(t);
      }
    }
  }

  const edges: Dependency[] = [];
  for (const [key, typesSet] of edgeKeyToTypes) {
    const [from, to] = key.split("->");
    edges.push({ from, to, types: [...typesSet] });
  }

  return { nodes: components, edges };
}

export function computeLayers(components: DUUIComponent[], edges: Dependency[]) {
  const ids = components.map(c => c.id);

  const preds = new Map<string, string[]>();
  const succs = new Map<string, string[]>();
  const inDegree = new Map<string, number>();

  for (const id of ids) {
    preds.set(id, []);
    succs.set(id, []);
    inDegree.set(id, 0);
  }

  for (const e of edges) {
    succs.get(e.from)!.push(e.to);
    preds.get(e.to)!.push(e.from);
    inDegree.set(e.to, (inDegree.get(e.to) ?? 0) + 1);
  }

  // Kahn topo + layer
  const queue: string[] = [];
  const layer = new Map<string, number>();

  for (const [id, deg] of inDegree) {
    if (deg === 0) {
      queue.push(id);
      const comp = components.find((c) => c.id === id);
      const hasInputs = !!comp && (comp.inputs?.length ?? 0) > 0;
      // Nodes with no inputs at all are true sources (layer 0),
      // nodes that require pre-annotated input start one layer below.
      layer.set(id, hasInputs ? 1 : 0);
    }
  }

  const topoOrder: string[] = [];

  while (queue.length) {
    const id = queue.shift()!;
    topoOrder.push(id);

    for (const succ of succs.get(id) ?? []) {
      // update succ layer based on this predecessor
      const currentLayer = layer.get(succ) ?? 0;
      const newLayer = Math.max(currentLayer, (layer.get(id) ?? 0) + 1);
      layer.set(succ, newLayer);

      inDegree.set(succ, (inDegree.get(succ) ?? 0) - 1);
      if (inDegree.get(succ) === 0) {
        queue.push(succ);
      }
    }
  }

  if (topoOrder.length !== ids.length) {
    throw new Error("Graph has a cycle (pipeline invalid)");
  }

  return { layer, topoOrder };
}

export function groupByLayer(
  components: DUUIComponent[],
  layer: Map<string, number>,
  topoOrder: string[]
) {
  const byLayer = new Map<number, string[]>();

  // ensure deterministic order: use topoOrder
  for (const id of topoOrder) {
    const l = layer.get(id) ?? 0;
    if (!byLayer.has(l)) byLayer.set(l, []);
    byLayer.get(l)!.push(id);
  }

  // normalize to an array sorted by layer
  const layers = Array.from(byLayer.entries())
    .sort(([a], [b]) => a - b)
    .map(([layerIndex, nodeIds]) => ({ layerIndex, nodeIds }));

  return layers;
}




/**
 * Create a blank component given a pipeline Id and an index in that pipeline.
 *
 * @param pipelineId The id of the pipeline the component is added to.
 * @param index The position in the pipeline.
 * @returns A default component.
 */
export const blankComponent = (pipelineId: string, index: number) =>
	<DUUIComponent>{
		oid: uuidv4(),
		id: uuidv4(),
		name: 'New Component ' + (index + 1),
		tags: [],
		description: '',
		driver: DUUIDockerDriver,
		target: '',
		inputs: [],
		outputs: [],
		options: {
			use_GPU: false,
			docker_image_fetching: true,
			scale: 1,
			keep_alive: false,
			registry_auth: {
				username: '',
				password: ''
			},
			constraints: [],
			labels: [],
			host: '',
			ignore_200_error: true
		},
		parameters: {},
		pipeline_id: pipelineId,
		user_id: null,
		index: index
	}

/**
 * Convert a DUUIComponent to a JSON object with some properties omitted.
 *
 * @param component the component to convert
 * @returns An object with relevant component properties.
 */
export const componentToJson = (component: DUUIComponent) => {
	return {
		name: component.name,
		tags: component.tags,
		description: component.description,
		driver: component.driver,
		target: component.target,
		inputs: component.inputs, 
		outputs: component.outputs,
		options: component.options,
		parameters: component.parameters
	}
}

export const splitClass = (fqn: string) => {
		const parts = fqn.split('.')
		const simple = parts.pop() || fqn
		const pkg = parts.join('.')
		return { simple, pkg }
	}
