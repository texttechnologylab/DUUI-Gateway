<!--	
	@component
	A component to edit DUUIComponents that appears on the right side on the screen (Sidebar Drawer).
-->
<script lang="ts">
    import {
        DUUIDrivers,
        type DUUIComponent,
        componentToJson,
        type DUUIRegistryEntryList,
        type DUUIRegistryEntry, type DUUIComponentMetaData, RegistryDrivers
    } from '$lib/duui/component'
	import { errorToast, successToast } from '$lib/duui/utils/ui'
	import {
		currentPipelineStore,
		exampleComponent,
		examplePipelineStore,
		userSession
	} from '$lib/store'
	import { showConfirmationModal } from '$lib/svelte/utils/modal'
	import {
		faAngleDoubleRight,
		faCancel,
		faFileCircleCheck,
		faFileExport,
		faFileUpload,
		faTrash
	} from '@fortawesome/free-solid-svg-icons'
	import { getDrawerStore, getModalStore, getToastStore } from '@skeletonlabs/skeleton'
	import pkg from 'lodash'
    import {createEventDispatcher, onMount} from 'svelte'
	import Fa from 'svelte-fa'
	import { v4 as uuidv4 } from 'uuid'
	import DriverIcon from '../DriverIcon.svelte'
	import Chips from '../Input/Chips.svelte'
	import ComponentOptions from '../Input/ComponentOptions.svelte'
	import Dropdown from '../Input/Dropdown.svelte'
	import JsonInput from '../Input/JsonInput.svelte'
	import TextArea from '../Input/TextArea.svelte'
	import TextInput from '../Input/TextInput.svelte'
	import Tip from '../Tip.svelte'
	import JsonDropdownInput from "$lib/svelte/components/Input/JsonDropdownInput.svelte";
	import RegistryDropdown from "$lib/svelte/components/Input/RegistryDropdown.svelte";
    const { cloneDeep, isEmpty } = pkg

	const defaultRegistryMeta: DUUIComponentMetaData = {
		tag: 'latest',
        search_tags: [],
        documentation: '',
        description: '',
        short_description: '',
        references: [],
        language: [],
        required_types: [],
        resulting_types: []
    }

    // Remove the `/v2` segment that the registry API currently prepends and collapse duplicate slashes.
    const sanitizeRegistryUrl = (value?: string) => {
        if (!value) return ''
        const trimmed = value.trim()

        const schemeMatch = trimmed.match(/^[a-zA-Z][a-zA-Z\d+\-.]*:\/\//)
        const protocol = schemeMatch ? schemeMatch[0] : ''
        let rest = schemeMatch ? trimmed.slice(protocol.length) : trimmed

        rest = rest.replace(/\/v2(?=\/|$)/g, '/').replace(/^v2(?=\/|$)/g, '')
        rest = rest.replace(/\/{2,}/g, '/')
        rest = rest.replace(/\/+$/, '')

        if (protocol && rest.startsWith('/')) {
            rest = rest.slice(1)
        }

        return `${protocol}${rest}`
    }

	const drawerStore = getDrawerStore()
	let component: DUUIComponent = $drawerStore.meta.component
	const inEditor = $drawerStore.meta.inEditor
	const creating = $drawerStore.meta.creating
	const example = $drawerStore.meta.example

	const modalStore = getModalStore()
	const toastStore = getToastStore()

	let parameters: Map<string, string> = new Map(Object.entries(component.parameters))
	const dispatcher = createEventDispatcher()

	let lastSuggestedTarget = ''
	let registrySelectionActive = false

	let labels: string[]
	component.options.labels = component.options.labels ? component.options.labels : []
	let chosenLabels: Map<string, string> = new Map()
	for (let l=0; l<component.options.labels.length; l++) {
		chosenLabels.set(component.options.labels[l], component.options.labels[l])
	}

	const fetchDriverLabels = async () => {
		const response = await fetch(`/api/users/labels/?driver=${component.driver}`, {
			method: 'GET'
		})

		if (response.ok) {
			const result = await response.json()
			labels = result["labels"]

			if (!labels || !labels.length) {
				component.options.labels = []
			} else {
				labels = [...labels]
				component.options.labels = component.options.labels.filter((label) => labels.includes(label))
			}
			
			for (let l=0; l<component.options.labels.length; l++) {
				chosenLabels.set(component.options.labels[l], component.options.labels[l])
			}

			chosenLabels.clear() // Clear the map
			component.options.labels.forEach(label => {
				chosenLabels.set(label, label); // Repopulate the map
			});
			chosenLabels = new Map(chosenLabels); // Create a new Map instance
			// component.options.labels = [...component.options.labels] // Trigger reactivity
			}
	}

	fetchDriverLabels()

	const onUpdate = async () => {
		if (!inEditor) {
			component.parameters = Object.fromEntries(parameters)
			const response = await fetch('/api/components', {
				method: 'PUT',
				body: JSON.stringify(component)
			})

			if (response.ok) {
				toastStore.trigger(successToast('Component updated successfully'))
				parameters = new Map(Object.entries(component.parameters))
				chosenLabels = new Map()
				for (let l=0; l<component.options.labels.length; l++) {
					chosenLabels.set(component.options.labels[l], component.options.labels[l])
				}
				chosenLabels = chosenLabels
			}

			dispatcher('updated', { ...component })
			if (component.pipeline_id) {
				$currentPipelineStore.components.forEach((c, index) => (c.index = index))
				component.id = uuidv4()
				$currentPipelineStore.components[component.index] = component
			}
		}

		drawerStore.close()
	}

	const uploadTemplate = async () => {
		const response = await fetch('/api/components?template=true', {
			method: 'POST',
			body: JSON.stringify({
				...component,
				index: component.index || $currentPipelineStore.components.length,
				pipeline_id: $currentPipelineStore.oid
			})
		})

		if (response.ok) {
			toastStore.trigger(successToast('Uploaded'))
			drawerStore.close()
		} else {
			toastStore.trigger(errorToast(response.statusText))
		}
	}


	const onCreate = async () => {
		if (example) {
			$exampleComponent = { ...component }
			$currentPipelineStore.components[component.index] = $exampleComponent
			$currentPipelineStore.components = $currentPipelineStore.components
		} else {
			component.parameters = Object.fromEntries(parameters)

			const response = await fetch('/api/components', {
				method: 'POST',
				body: JSON.stringify({
					...component,
					index: component.index || $currentPipelineStore.components.length,
					pipeline_id: $currentPipelineStore.oid
				})
			})

			if (response.ok) {
				const result: DUUIComponent = await response.json()
				$currentPipelineStore.components.splice(component.index, 0, { ...result, id: uuidv4() })
				$currentPipelineStore.components = $currentPipelineStore.components.map(
					(c: DUUIComponent) => {
						return { ...c, index: $currentPipelineStore.components.indexOf(c) }
					}
				)
			} else {
				errorToast(response.statusText)
			}
		}

		drawerStore.close()
	}

	const onEdited = async () => {

		component.parameters = Object.fromEntries(parameters)

		if (creating) {
			const copy = cloneDeep(component)
			copy.id = uuidv4()
			copy.index = component.index || $currentPipelineStore.components.length
			$currentPipelineStore.components.splice(copy.index, 0, copy)
			$currentPipelineStore.components = $currentPipelineStore.components.map(
				(c: DUUIComponent) => {
					return { ...c, index: $currentPipelineStore.components.indexOf(c) }
				}
			)
		} else {
			$currentPipelineStore.components[component.index] = component
		}

		drawerStore.close()
	}

	const onRemove = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Remove Component',
				message: `Are you sure you want to remove ${component.name}?`,
				textYes: 'Remove'
			},
			modalStore
		)

		if (!confirm) return

		if (example) {
			$examplePipelineStore.components = $examplePipelineStore.components.filter(
				(c) => c.id !== component.id
			)
		} else {
			$currentPipelineStore.components = $currentPipelineStore.components.filter(
				(c) => c.id !== component.id
			)
		}

		drawerStore.close()
	}

	const onDelete = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Component',
				message: `Are you sure you want to delete ${component.name}?`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (!confirm) return

		const response = await fetch('/api/components', {
			method: 'DELETE',
			body: JSON.stringify(component)
		})
		if (response.ok) {
			toastStore.trigger(successToast('Component deleted successfully'))

			$currentPipelineStore.components = $currentPipelineStore.components.filter(
				(c) => c.id !== component.id
			)

			dispatcher('deleteComponent', { oid: component.oid })
		} else {
			toastStore.trigger(errorToast('Error: ' + response.statusText))
		}

		drawerStore.close()
	}

	type DockerCheckStatus = 'idle' | 'pending' | 'valid' | 'invalid' | 'timeout' | 'warning'
	type DockerCheckState = {
		status: DockerCheckStatus
		message: string
	}

	type DockerCheckResult = {
		ok: boolean
		timedOut?: boolean
		message?: string
		warning?: boolean
	}

	const requiresDockerRegistry = (driver: string): boolean => RegistryDrivers.includes(driver)

	let dockerCheck: DockerCheckState = { status: 'idle', message: '' }
	let dockerCheckAbort: AbortController | null = null
	let dockerCheckDebounce: ReturnType<typeof setTimeout> | null = null

	const resetDockerCheck = () => {
		if (dockerCheckAbort) {
			dockerCheckAbort.abort()
			dockerCheckAbort = null
		}
		if (dockerCheckDebounce) {
			clearTimeout(dockerCheckDebounce)
			dockerCheckDebounce = null
		}
		dockerCheck = { status: 'idle', message: '' }
	}

	const cancelDockerCheck = () => {
		resetDockerCheck()
	}

	const checkDockerImageAvailability = async (
		imageUri: string,
		signal?: AbortSignal,
		timeoutMs: number | null = 5000
	): Promise<DockerCheckResult> => {
		if (!imageUri?.trim()) return { ok: false }

		try {
			const response = await fetch('/api/components/registry', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					imageUri,
					timeoutMs
				}),
				signal
			})

			const payload = (await response.json().catch(() => null)) as DockerCheckResult | null
			if (!payload) {
				return {
					ok: false,
					message: 'Invalid response from verification service.'
				}
			}

			return payload
		} catch (error) {
			if ((error as DOMException).name === 'AbortError') {
				throw error
			}

			return {
				ok: false,
				warning: true,
				message: 'Failed to contact verification service.'
			}
		}
	}

	const runDockerCheck = (target: string, timeoutMs: number | null = 5000, debounce = false) => {
		if (!target?.trim()) {
			resetDockerCheck()
			return
		}

		if (dockerCheckDebounce) {
			clearTimeout(dockerCheckDebounce)
			dockerCheckDebounce = null
		}

		const executeCheck = () => {
			dockerCheck = { status: 'pending', message: '' }
			if (dockerCheckAbort) {
				dockerCheckAbort.abort()
			}

			const controller = new AbortController()
			const { signal } = controller
			dockerCheckAbort = controller

			checkDockerImageAvailability(target, signal, timeoutMs)
				.then((result) => {
					if (signal.aborted) return
					if (result.timedOut) {
						dockerCheck = {
							status: 'timeout',
							message: 'Image check timed out. Retry without timeout if you trust the registry.'
						}
					} else if (result.ok) {
						dockerCheck = { status: 'valid', message: '' }
					} else if (result.warning) {
						dockerCheck = {
							status: 'warning',
							message:
								result.message ||
								'Could not verify Docker image because the request failed unexpectedly.'
						}
					} else {
						dockerCheck = {
							status: 'invalid',
							message:
								result.message ||
								'Docker image not reachable. Please ensure the image is available.'
						}
					}
				})
				.catch((error) => {
					if ((error as DOMException).name === 'AbortError') {
						return
					}

					if (error instanceof TypeError) {
						dockerCheck = {
							status: 'warning',
							message: 'Could not contact the verification service. Please check your connection.'
						}
						return
					}

					dockerCheck = {
						status: 'invalid',
						message: 'Failed to verify Docker image. Please check the URL and network.'
					}
				})
				.finally(() => {
					if (dockerCheckAbort === controller) {
						dockerCheckAbort = null
					}
				})
		}

		if (debounce) {
			dockerCheckDebounce = setTimeout(executeCheck, 500)
		} else {
			executeCheck()
		}
	}

	$: {
		if (!requiresDockerRegistry(component.driver) || !component.target?.trim()) {
			resetDockerCheck()
		} else {
			runDockerCheck(component.target, 5000, true)
		}
	}

	const exportComponent = () => {
		const blob = new Blob([JSON.stringify(componentToJson(component))], {
			type: 'application/json'
		})
		const url = URL.createObjectURL(blob)
		const anchor = document.createElement('a')
		anchor.href = url
		anchor.download = `${component.name}.json`
		document.body.appendChild(anchor)
		anchor.click()
		document.body.removeChild(anchor)
		URL.revokeObjectURL(url)
	}

</script>

<div class="menu-mobile lg:!max-w-[50%]">
	<button class="button-mobile" on:click={drawerStore.close}>
		<Fa icon={faAngleDoubleRight} size="lg" />
		<span>Close</span>
	</button>

	{#if example}
		<button
			disabled={
				!component.driver ||
				!component.name ||
				!component.target ||
				(requiresDockerRegistry(component.driver) &&
					['pending', 'invalid', 'timeout'].includes(dockerCheck.status))
			}
			class="button-mobile"
			on:click={onCreate}
		>
			<Fa icon={faFileCircleCheck} />
			Save
		</button>
		<button type="button" class="button-mobile" on:click={onRemove}>
			<Fa icon={faTrash} />
			Remove
		</button>
	{:else if creating}
		<button
			disabled={
				!component.driver ||
				!component.name ||
				!component.target ||
				(requiresDockerRegistry(component.driver) &&
					['pending', 'invalid', 'timeout'].includes(dockerCheck.status))
			}
			class="button-mobile"
			on:click={() => (inEditor ? onEdited() : onCreate())}
		>
			<Fa icon={faFileCircleCheck} />
			Confirm
		</button>
		<button type="button" class="button-mobile" on:click={drawerStore.close}>
			<Fa icon={faCancel} />
			Cancel
		</button>
	{:else}
		<button
			class="button-mobile"
			on:click={() => (inEditor ? onEdited() : onUpdate())}
			disabled={
				!component.driver ||
				!component.name ||
				!component.target ||
				(requiresDockerRegistry(component.driver) &&
					['pending', 'invalid', 'timeout'].includes(dockerCheck.status))
			}
		>
			<Fa icon={faFileCircleCheck} />
			Save
		</button>
		<button
			type="button"
			class="button-mobile"
			on:click={() => (inEditor ? onRemove() : onDelete())}
		>
			<Fa icon={faTrash} />
			Delete
		</button>
	{/if}
</div>

<div class="bg-surface-50-900-token h-screen relative">
	<div
		class="p-4 space-y-4 justify-between sticky top-0 z-10 bg-surface-100-800-token border-b border-color"
	>
		<div class="flex-center-4">
			<div class="flex-center-4">
				<DriverIcon driver={component.driver} />
				<h3 class="h3">{component.name}</h3>
			</div>
		</div>

		<div class="hidden md:flex space-x-2">
			{#if $userSession?.role === 'Admin'}
				<button class="button-neutral" on:click={uploadTemplate}>
					<Fa icon={faFileUpload} />
					<span>Upload as Template</span>
				</button>
			{/if}
			{#if example}
				<button
					disabled={
						!component.driver ||
						!component.name ||
						!component.target ||
						(requiresDockerRegistry(component.driver) &&
							['pending', 'invalid', 'timeout'].includes(dockerCheck.status))
					}
					class="button-neutral"
					on:click={onCreate}
				>
					<Fa icon={faFileCircleCheck} />
					<span>Save</span>
				</button>
				<button type="button" class="button-neutral" on:click={onRemove}>
					<Fa icon={faTrash} />
					<span>Remove</span>
				</button>
			{:else if creating}
				<button
					disabled={
						!component.driver ||
						!component.name ||
						!component.target ||
						(requiresDockerRegistry(component.driver) &&
							['pending', 'invalid', 'timeout'].includes(dockerCheck.status))
					}
					class="button-neutral ml-auto"
					on:click={() => (inEditor ? onEdited() : onCreate())}
				>
					<Fa icon={faFileCircleCheck} />
					Confirm
				</button>
				<button type="button" class="button-neutral" on:click={drawerStore.close}>
					<Fa icon={faCancel} />
					Cancel
				</button>
			{:else}
				<button class="button-neutral" on:click={exportComponent}>
					<Fa icon={faFileExport} />
					<span>Export</span>
				</button>
				<button
					class="button-neutral"
					on:click={() => (inEditor ? onEdited() : onUpdate())}
					disabled={
						!component.driver ||
						!component.name ||
						!component.target ||
						(requiresDockerRegistry(component.driver) &&
							['pending', 'invalid', 'timeout'].includes(dockerCheck.status))
					}
				>
					<Fa icon={faFileCircleCheck} />
					Save
				</button>
				<button
					type="button"
					class="button-neutral"
					on:click={() => (inEditor ? onRemove() : onDelete())}
				>
					<Fa icon={faTrash} />
					{inEditor ? 'Remove' : 'Delete'}
				</button>
			{/if}
		</div>
	</div>

	<div class="space-y-8 p-4 bg-surface-50-900-token pb-16 md:pb-4">
		<div class="space-y-4">
			<h4 class="h4">Properties</h4>

			<TextInput label="Name" name="name" bind:value={component.name} />

			<Dropdown label="Driver" name="driver" options={DUUIDrivers} bind:value={component.driver} on:change={fetchDriverLabels} />

			{#if RegistryDrivers.includes(component.driver)}
			<div class="flex">

				<RegistryDropdown
					on:change={
						(event) => {

						if (!event.detail || !event.detail.entry) return;

						const registryEntry = event.detail.entry
						const entryMetadata = event.detail.metaData ?? defaultRegistryMeta
						const metadataTag = entryMetadata.tag && entryMetadata.tag.trim().length ? entryMetadata.tag : defaultRegistryMeta.tag

						if (!registryEntry.name) {
							return
						}

						if (isEmpty(registryEntry.name)) return;

						// Build a suggested target from the registry entry.
						// If the registry URL is missing, fall back to name:tag.
						const baseRegistryUrl = sanitizeRegistryUrl(registryEntry.registry_url)
						let suggestedTarget = baseRegistryUrl
							? `${baseRegistryUrl}/${registryEntry.name}:${metadataTag}`
							: `${registryEntry.name}:${metadataTag}`;

						if (suggestedTarget.startsWith('https://')) {
							suggestedTarget = suggestedTarget.replace(/^https?:\/\//, '');
						}

						component.name = registryEntry.name
						component.target = suggestedTarget;
						component.description = entryMetadata.description
						lastSuggestedTarget = suggestedTarget

						}
					}
				/>
			</div>

			{/if}

			<div class="space-y-4 group">
				{#if requiresDockerRegistry(component.driver)}
					{#if dockerCheck.status === 'invalid'}
						<Tip tipTheme="error">
							{dockerCheck.message}
							</Tip>
						{:else if dockerCheck.status === 'timeout'}
							<Tip tipTheme="tertiary">
								<div class="flex flex-wrap items-center justify-between gap-2">
									<span>{dockerCheck.message}</span>
									<div class="flex flex-wrap gap-2">
										<button
											type="button"
											class="button-warning"
											on:click={() => runDockerCheck(component.target, null)}
										>
											Retry (no timeout)
										</button>
										<button
											type="button"
											class="button-neutral"
											on:click={cancelDockerCheck}
										>
											Cancel
										</button>
									</div>
								</div>
							</Tip>
						{:else if dockerCheck.status === 'pending'}
							<Tip tipTheme="primary">
								<div class="flex flex-wrap items-center justify-between gap-2">
									<span>Checking image availability…</span>
									<button type="button" class="button-neutral" on:click={cancelDockerCheck}>
										Cancel
									</button>
								</div>
							</Tip>
						{:else if dockerCheck.status === 'warning'}
							<Tip tipTheme="warning">
								<div class="flex flex-wrap items-center justify-between gap-2">
									<span>{dockerCheck.message}</span>
									<button
										type="button"
										class="button-neutral"
										on:click={() => runDockerCheck(component.target, null)}
									>
										Retry anyway
									</button>
								</div>
							</Tip>
						{/if}
					{/if}

					<TextInput
						style="md:col-span-2"
						label="Target"
						name="target"
						bind:value={component.target}
						error={component.target === '' ? "Target can't be empty" : ''}
					>
						<span slot="labelContent" class="flex w-full items-center justify-between gap-3">
							<span>Target</span>
							{#if dockerCheck.status === 'valid'}
								<span class="badge variant-soft-success font-bold text-xs tracking-wide">
									IMAGE ONLINE
								</span>
							{/if}
						</span>
					</TextInput>

					<div class="hidden group-focus-within:block">
						<Tip>
							The target can be a Docker image name (Docker, Swarm and Kubernetes Driver) or a URL
							(Remote Driver). 
							 <!-- or a Java class path (UIMADriver). -->
					</Tip>
				</div>
			</div>


			<Chips style="md:col-span-2" label="Tags" bind:values={component.tags} />
			<TextArea
				style="md:col-span-2"
				label="Description"
				name="description"
				bind:value={component.description}
			/>
		</div>

		<div class="space-y-4 border-t border-color pt-4">
			<h4 class="h4">Options</h4>
			<ComponentOptions {component} />
		</div>

		{#if labels && labels.length > 0}
			<div class="space-y-2 ">
				<h4 class="h4">Labels</h4>
				<JsonDropdownInput bind:dropdownList={labels} bind:target={component.options.labels} bind:data={chosenLabels}/>
			</div>
		{/if}
		<div class="space-y-4 border-t border-color pt-4">
			<h4 class="h4">Parameters</h4>
			<JsonInput bind:data={parameters} />
		</div>
	</div>
</div>
