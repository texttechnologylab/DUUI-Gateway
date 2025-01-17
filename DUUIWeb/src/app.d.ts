// See https://kit.svelte.dev/docs/types#app
// for information about these interfaces
// and what to do when importing types
declare global {
	declare namespace App {
		interface Locals {
			user: User
			globals: Globals
		}
	}
}

// Types related to a User
type User = UserProperties | null | undefined

interface UserProperties {
	oid: string
	name?: string
	email?: string
	password?: string
	role: 'None' | 'User' | 'Admin' | 'Trial' | 'System'
	session: string
	expires?: string
	preferences: {
		tutorial: boolean
		step: number
	}
	connections: {
		key: string | null
		dropbox: OAuthConnections
		minio: MinioConnections
		mongoDB: {
			uri: string | null
			host: string | null
			port: string | null
			username: string | null
			password: string | null
		}
		nextcloud?: NextCloudConnections
		google?: OAuthConnections
	}
}

export type ServiceConnections = {
	[name: string]: OAuthConnectionDetails | MinioConnectionDetails | NextcloudConnectionDetails;
};

export type OAuthConnections = {
	[name: string]: OAuthConnectionDetails;
};
export type MinioConnections = {
	[name: string]: MinioConnectionDetails;
};
export type NextCloudConnections = {
	[name: string]: NextcloudConnectionDetails;
};

export type OAuthConnectionDetails = {
	alias: string
	access_token: string | null
	refresh_token: string | null
};

export type MinioConnectionDetails = {
	alias: string
	endpoint: string | null
	access_key: string | null
	secret_key: string | null
};

export type NextcloudConnectionDetails = {
	alias: string
	uri: string | null
	username: string | null
	password: string | null
}

export const defaultGlobals: () => Globals = () => {
	return {
		roles: {
			"Admin": {
				priority: 1,
				modifiable: "False"
			},
			"User": {
				priority: 2,
				modifiable: "False"
			}
		},
		list: {
			languages: {
				items: ["de", "en", "fr"]
			}
		},
		feature_restrictions: {
			"process_action_read_document": {
				restriction: 2
			}
		},
		content: {
			"how_to_cite": {
				text: "@inproceedings{Leonhardt:et:al:2023,\n\ttitle = {Unlocking the Heterogeneous Landscape of Big Data {NLP} with {DUUI}},\n\tauthor = {Leonhardt, Alexander and Abrami, Giuseppe and Baumartz, Daniel and Mehler, Alexander},\n\teditor = {Bouamor, Houda and Pino, Juan and Bali, Kalika},\n\tbooktitle = {Findings of the Association for Computational Linguistics: EMNLP 2023},\n\tyear = {2023},\n\taddress = {Singapore},\n\tpublisher = {Association for Computational Linguistics},\n\turl = {https://aclanthology.org/2023.findings-emnlp.29},\n\tpages = {385--399},\n\tpdf = {https://aclanthology.org/2023.findings-emnlp.29.pdf},\n\tabstract = {Automatic analysis of large corpora is a complex task, especially\nin terms of time efficiency. This complexity is increased by the\nfact that flexible, extensible text analysis requires the continuous\nintegration of ever new tools. Since there are no adequate frameworks\nfor these purposes in the field of NLP, and especially in the\ncontext of UIMA, that are not outdated or unusable for security\nreasons, we present a new approach to address the latter task:\nDocker Unified UIMA Interface (DUUI), a scalable, flexible, lightweight,\nand feature-rich framework for automatic distributed analysis\nof text corpora that leverages Big Data experience and virtualization\nwith Docker. We evaluate DUUI{'}s communication approach against\na state-of-the-art approach and demonstrate its outstanding behavior\nin terms of time efficiency, enabling the analysis of big text\ndata.}\n}"
			},
			"ttlab_github": {
				link: "https://github.com/texttechnologylab"
			}
		}
	}
}

export type Globals = null | undefined | {
	roles: Roles
	lists: Lists
	feature_restrictions: FeatureRestrictions
	content: UserFacingContent
}
// isAllowed, getContent()

export const isAllowed = (featureId:string, globals: Globals, role: string) => {
	const userRole = globals.roles[role]
	const userPriority = userRole ? userRole.priority : globals.roles["User"].priority
	const  feature = globals.feature_restrictions[featureId]
	const featureRestriction = feature ? feature.restriction : Infinity

	return userPriority <= featureRestriction
}

export const getContent = (contentId:string, globals: Globals) => {
	return globals.content[contentId]
}

export const getList = (listId: string, globals: Globals) => {
	return globals.lists[listId].items
}

type Roles = {
	[name: string]: {priority: number, modifiable: boolean}
}

type RoleRestriction = {
	restriction: number
}

type Lists = {
	[list_id: string]: {
		items: string[]
	}
}

type FeatureRestrictions = {
	[feature_id: string]: RoleRestriction
}

type UserFacingContent = {
	[content_id: string]: {
		text?: string
		html?: string
		icon?: string
		link?: string
	}
}

// Types related to the pagination of tables
type PaginationSettings = {
	page: number = 0
	limit: number = 20
	total: number
	sizes: number[] = [5, 10, 20, 50]
}

enum Order {
	Ascending = 1,
	Descending = -1
}

type Sort = {
	index: number
	order: Order
}

// Types related to the documentation of the api at /documentation/api
type AggregationStep = { _id: string; count: number[] }
type AggreationResult = AggreationStep[]

type APIMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
type ParameterType = 'Query' | 'Body'
type APIParameter = { name: string; type: ParameterType; description: string }

type APIEndpoint = {
	method: APIMethod
	route: string
	returns: { code: number; message: string }[]
	parameters: APIParameter[]
	description: string
	exampleRequest: string = ''
}

// Types related to Feedback

type Score = 1 | 2 | 3 | 4 | 5 | 6 | 7

type Feedback = {
	name: string
	message: string
	step: number
	language: 'german' | 'english'
	programming: number
	nlp: number
	nlpNeeded: boolean
	duui: boolean
	duuiRating: number
	java: number
	python: number
	requirements: number
	frustration: number
	ease: number
	correction: number
}

type FeedbackResult = {
	programming: number
	nlp: number
	nlpNeeded: boolean
	duui: boolean
	duuiRating: number
	java: number
	python: number
	requirements: number
	frustration: number
	ease: number
	correction: number
	timestamp: number
}
