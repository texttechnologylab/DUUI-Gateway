<!--	
	@component
	A component that displays metrics and other information about a DUUIDocument
-->
<script lang="ts">
	import { includes } from '$lib/duui/utils/text'
	import { errorToast } from '$lib/duui/utils/ui'
	import { isDarkModeStore, userSession, globalsSession } from '$lib/store'
	import type { FeatureRestrictions, Lists, UserFacingContent } from '../../../../app'
	import {
		faCheck,
		faChevronDown,
		faChevronLeft, faChevronRight,
		faClose,
		faDownload,
		faRefresh
	} from '@fortawesome/free-solid-svg-icons'
	import { getDrawerStore, getToastStore } from '@skeletonlabs/skeleton'
	import { onMount } from 'svelte'
	import Number from '../Input/Number.svelte'
	import Search from '../Input/Search.svelte'
	import Dropdown from "$lib/svelte/components/Input/Dropdown.svelte";
	import Fa from 'svelte-fa'
	import MapDropdown from '$lib/svelte/components/Input/MapDropdown.svelte'

	const drawerStore = getDrawerStore()

	const toastStore = getToastStore()

	export let title: string
	export let items: UserFacingContent | Lists | FeatureRestrictions

	let globals = $globalsSession
	let itemsExpanded = true
	let searchText = ''
	let itemFilter: Map<string, any> = new Map(Object.entries(items || {}))


	$: {
		if (items) {
			itemFilter = new Map(
				Object.entries(items).filter(
					(entry) =>
						(includes(entry[0], searchText) || searchText === '') // TODO: Add filter for content type
				)
			)
		}
	}
</script>

<div class="h-full bg-surface-50-900-token">
	<div id="scroll-top" />
		<div
			class="w-full z-50 grid
			font-bold text-2xl p-4 border-surface-200 dark:border-surface-500 sm:flex items-center justify-start gap-4 sticky top-0 bg-surface-100-800-token border-b border-color"
		>
			<div class="bg-surface-50-900-token">

				<div class="p-4 flex flex-col gap-4 border-b border-color">
					{#if items && Object.entries(items).length > 0}
						<h2 class="h2">{ title }</h2>
						<div class="flex items-end gap-4 justify-end">
							<button
								class="button-neutral mr-auto"
								on:click={() => (itemsExpanded = !itemsExpanded)}
							>
								<div class:turn={itemsExpanded} class="transition-transform duration-300">
									<Fa icon={faChevronDown} size="lg" />
								</div>
								<span>{itemsExpanded ? 'Collapse' : 'Expand'}</span>
							</button>
							<div class="grid md:grid-cols-2 items-end gap-4">
								<Search placeholder="Search" bind:query={searchText} />
							</div>
						</div>

						<div class:open={itemsExpanded} class="content dimmed">
							<div class="content-wrapper space-y-4">
								<div
									class="self-stretch text-sm overflow-hidden grid sm:grid-cols-2 xl:grid-cols-3 gap-4"
								>
									{#each itemFilter.entries() as [name, item]}
										<div class="grid grid-cols-2 card gap-4 p-4">
											<div class="flex flex-col items-start justify-center gap-2">
												<p class="font-bold">Identifier</p>
												<p class="">{name}</p>
											</div>
											<div class="flex flex-col items-start justify-center gap-2">
												<p class="font-bold">Permission</p>
												<MapDropdown
													bind:options={globals?.roles || {}}
													bind:value={globals?["content"]?[name]["permission"]["restriction"]}
												></MapDropdown>
											</div>
											<div class="flex flex-row items-start justify-center gap-2">
												{#if !!globals["content"][name]?.text }

												{/if}
											</div>
										</div>
									{/each}
								</div>
							</div>
						</div>
					{/if}
				</div>
			</div>

		</div>
</div>

<style>
	.content-wrapper {
		overflow: hidden;
	}

	.content {
		display: grid;
		grid-template-rows: 0fr;
		transition: grid-template-rows 300ms;
	}

	.open {
		grid-template-rows: 1fr;
	}

	.turn {
		transform: rotate(-180deg);
	}
</style>
