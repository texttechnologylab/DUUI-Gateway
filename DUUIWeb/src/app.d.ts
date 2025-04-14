// See https://kit.svelte.dev/docs/types#app
// for information about these interfaces

// import type { DUUIDriver, DUUIDriverFilter } from "$lib/duui/component"


/// <reference types="@sveltejs/kit" />

declare namespace App {
	interface Locals {
		user: User
	}
	interface Platform {
		env: {
		  API_URL: string
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
		nextcloud: NextCloudConnections
		google: OAuthConnections
	}
}

type DUUILabels = {
	[labelId: string]: DUUILabel
}

type DUUILabel = {
	label: string
	driver: DUUIDriverFilter
	scope: string
	groups: string[]
}

type DUUIGroup = {
	name: string
	members: string[]
}

type DUUIGroups = {
	[groupId: string]: DUUIGroup
}

type ServiceConnections = {
	[name: string]: OAuthConnectionDetails | MinioConnectionDetails | NextcloudConnectionDetails;
};

type OAuthConnections = {
	[name: string]: OAuthConnectionDetails;
};
type MinioConnections = {
	[name: string]: MinioConnectionDetails;
};
type NextCloudConnections = {
	[name: string]: NextcloudConnectionDetails;
};

type OAuthConnectionDetails = {
	alias: string
	access_token: string | null
	refresh_token: string | null
};

type MinioConnectionDetails = {
	alias: string
	endpoint: string | null
	access_key: string | null
	secret_key: string | null
};

type NextcloudConnectionDetails = {
	alias: string
	uri: string | null
	username: string | null
	password: string | null
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
