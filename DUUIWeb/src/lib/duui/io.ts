/**
 * This file contains constants, types, interfaces and functions related to DUUI documents.
 */

import { equals } from '$lib/duui/utils/text'
import type { DUUIEvent } from './monitor'

export interface DUUIDocument {
	oid: string
	name: string
	path: string
	status: string
	progress: number
	is_finished: boolean
	error: string | null
	duration_decode: number
	duration_deserialize: number
	duration_wait: number
	duration_process: number
	duration: number
	size: number
	started_at: number
	finished_at: number
	annotations: {
		[key: string]: number
	}
	events: DUUIEvent[]
}

export type DUUIDocumentProvider = {
	provider: IOProvider
	path: string
	content: string
	file_extension: FileExtension
	provider_id: string;
}

export type IOProvider = 'Dropbox' | 'Minio' | 'File' | 'Text' | 'NextCloud' | 'Google'| 'LocalDrive' | 'None'
export type FileExtension = '.txt' | '.xmi' | '.gz'
export type OutputFileExtension = '.xmi' | '.gz'

export enum IO {
	Dropbox = 'Dropbox',
	File = 'File',
	Minio = 'Minio',
	Text = 'Text',
	NextCloud = 'NextCloud',
	Google = 'Google',
	LocalDrive = 'LocalDrive',
	None = 'None'
}

export const IO_INPUT: string[] = ['Dropbox', 'File', 'Minio', 'Text', 'NextCloud', 'Google', 'LocalDrive']
export const INPUT_EXTENSIONS: string[] = ['.txt', '.xmi', '.gz']

export const IO_OUTPUT: string[] = ['Dropbox', 'Minio', 'NextCloud', 'Google', 'LocalDrive', 'None']
export const OUTPUT_EXTENSIONS: string[] = ['.xmi', '.gz']

/**
 * Check if the input and output for the process are valid. This also includes checking
 * if the list of selected files is empty in case the input source is File.
 *
 * @param input The input settings
 * @param output The output settings
 * @param files The list of files that have been selected for processing.
 * @param user The current user's properties.
 * @returns
 */
export const isValidIO = (
	input: DUUIDocumentProvider,
	output: DUUIDocumentProvider,
	files: FileList,
	user: User
): boolean => {
	return isValidInput(input, files, user) && isValidOutput(output, user)
}

/**
 * Check if the input settings are valid.
 *
 * @param input The input settings for the process.
 * @param files The list of files that have to be uploaded as the data source.
 * @param user The current user's properties.
 * @returns whether the input settings are valid.
 */
export const isValidInput = (input: DUUIDocumentProvider, files: FileList, user: User): boolean => {

	if (isStatelessProvider(input.provider)) {
		if (equals(input.provider.toString(), IO.None)) {
			return false;
		}

		if (equals(input.provider.toString(), IO.Text)) {
			return input.content.length > 0
		}

		if (equals(input.provider.toString(), IO.File)) {
			return files?.length > 0
		}

		return true
	}

	if (equals(input.provider.toString(), IO.LocalDrive)) {
		return input.path.length > 0;
	}

	return isValidCloudProvider(input, user)

}

/**
 * Check if the settings for uploading files to a cloud storage are valid.
 * 
 * @param storage an object containg the name of the provider and the path to upload files to.
 * @returns if the storage object is valid.
 */
export const isValidFileUpload = (storage: { provider: IO; path: string }) => {
	if (equals(storage.provider, IO.Minio)) {
		return isValidS3BucketName(storage.path || '').length === 0
	}

	return !(equals(storage.provider, IO.Dropbox) && (storage.path === '/' || storage.path === ''));

}

/**
 * Check if the output settings are valid.
 *
 * @param output The output settings for the process.
 * @param user The current user's properties.
 * @returns whether the output settings are valid.
 */
export const isValidOutput = (output: DUUIDocumentProvider, user: User): boolean => {
	if (isStatelessProvider(output.provider)) {
		if (equals(output.provider.toString(), IO.None)) {
			return true
		}

		return true
	}

	if (equals(output.provider.toString(), IO.LocalDrive)) {
		return output.path.length > 0;
	}

	return isValidCloudProvider(output, user)
}

export const isStatelessProvider = (provider: IOProvider) => {
	return equals(provider.toString(), IO.File) ||
		equals(provider.toString(), IO.Text) ||
		equals(provider.toString(), IO.None)
}

export const hasFolderPicker = (provider: IOProvider, checkCloudProvider: boolean = false) => {
	return provider === IO.Dropbox || provider === IO.Google || provider === IO.NextCloud || (provider === IO.LocalDrive && !checkCloudProvider)
}

export const isValidCloudProvider = (provider: DUUIDocumentProvider, user: User) => {
	if (isStatelessProvider(provider.provider)) return true

	if (isOAuthProvider(provider.provider)) {
		return isValidOAuthProvider(provider, user)
	}

	if (equals(provider.provider.toString(), IO.Minio)) {
		return isValidMinioProvider(provider, user)
	}

	if (equals(provider.provider.toString(), IO.NextCloud)) {
		return isValidNextCloudProvider(provider, user)
	}
}

export const isValidMinioProvider = (provider: DUUIDocumentProvider, user: User) => {
	const minio = user?.connections.minio[provider.provider_id]

	return minio
		&& minio.alias.length > 0
		&& minio.endpoint !== null
		&& minio.access_key !== null
		&& minio.secret_key !== null
}

export const isValidNextCloudProvider = (provider: DUUIDocumentProvider, user: User) => {
	const nextcloud = user?.connections.nextcloud[provider.provider_id]

	return nextcloud
		&& nextcloud.alias.length > 0
		&& nextcloud.uri !== null
		&& nextcloud.username !== null
		&& nextcloud.password !== null
}

export const isValidOAuthProvider = (provider: DUUIDocumentProvider, user: User) => {

	const oauth = user?.connections[provider.provider.toLowerCase() as keyof ServiceConnections]

	return oauth
		&& oauth[provider.provider_id]?.alias?.length > 0
		&& oauth[provider.provider_id]?.access_token !== null
}

export const getCloudProviderAliases = (connections: ServiceConnections) : Map<string, string> => {
	return new Map(
		Object.entries(connections).map(([key, value]) => [key, value.alias] as [string, string])
	);
}

const isOAuthProvider = (provider: IOProvider) => provider === IO.Google || provider === IO.Dropbox
/**
 * Check if the process settings are valid.
 *
 * @param workerCount The maximum number of threads usable by the composer.
 * @param minimumSize The minimum file size in bytes.
 * @returns whether the settings are valid.
 */
export const areSettingsValid = (workerCount: number, minimumSize: number): string => {
	if (workerCount < 1 || workerCount > 20) {
		return 'Worker count must be between 1 and 20'
	}

	if (minimumSize < 0 || minimumSize > 2147483647) {
		return 'File size must be between 0 and 2147483647 bytes'
	}

	return ''
}

/**
 * Given a path as a string, check if the bucket name component of the path is valid. The bucket is expected to be
 * the part before the first forward slash or, if non exists the entire path.
 *
 * @param path The path to check
 * @returns
 */
export const isValidS3BucketName = (path: string): string => {
	if (path.startsWith('/')) return 'Bucket name must not begin with a forward slash'
	if (path.endsWith('/')) return 'Path name must not end with a forward slash'

	let bucket: string = ''

	if (!path.includes('/')) {
		bucket = path
	} else {
		const parts: string[] = path.split('/')
		if (parts.length === 1) return 'Bucket name can not be empty'
		bucket = parts.at(0) as string
	}

	if (bucket.length < 3 || bucket.length > 63)
		return 'Bucket name must be between 3 (min) and 63 (max) characters long'

	// Check valid characters and starting/ending with a letter or number
	if (!/^[a-zA-Z0-9]/.test(bucket) || !/[a-zA-Z0-9]$/.test(bucket)) {
		return 'Bucket name must begin and end with a letter or number'
	}

	// Check for valid characters (lowercase letters, numbers, dots, hyphens)
	if (!/^[a-z0-9.-]+$/.test(bucket)) {
		return 'Bucket name can consist only of lowercase letters, numbers, dots (.), and hyphens (-)'
	}

	// Check for adjacent periods
	if (/\.\./.test(bucket)) {
		return 'Bucket name must not contain two adjacent periods'
	}

	// Check for IP address format
	if (/^\d+\.\d+\.\d+\.\d+$/.test(bucket)) {
		return 'Bucket name must not be formatted as an IP address (for example, 192.168.5.4)'
	}

	// Check for prefix xn--
	if (bucket.startsWith('xn--')) {
		return 'Bucket name must not start with the prefix xn--'
	}

	// Check for prohibited prefixes
	if (bucket.startsWith('sthree-') || bucket.startsWith('sthree-configurator')) {
		return 'Bucket name must not start with the prefix sthree- or sthree-configurator'
	}

	// Check for reserved suffixes
	if (bucket.endsWith('-s3alias') || bucket.endsWith('--ol-s3')) {
		return 'Bucket name must not end with the suffix -s3alias or --ol-ss3'
	}

	return ''
}

/**
 * Helper function to sum the duration of all steps for a document.
 *
 * @param document a Document that is being or has been processed.
 * @returns the total duration in the pipeline.
 */
export const getTotalDuration = (document: DUUIDocument) => {
	return (
		document.duration_decode +
		document.duration_deserialize +
		document.duration_wait +
		document.duration_process
	)
}
