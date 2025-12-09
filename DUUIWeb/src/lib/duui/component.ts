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
