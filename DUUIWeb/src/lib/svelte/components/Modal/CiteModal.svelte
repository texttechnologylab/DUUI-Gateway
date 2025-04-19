<!--
	@component
	A modal component that displays component templates.
-->
<script lang="ts">
	import {clipboard, getModalStore, getToastStore} from '@skeletonlabs/skeleton'
	import { successToast } from '$lib/duui/utils/ui.js'
	import {faClipboard, faClose} from "@fortawesome/free-solid-svg-icons";
	import { Fa } from "svelte-fa";

	const modalStore = getModalStore()
	const toastStore = getToastStore()
	const citation = `
		@inproceedings{Leonhardt:et:al:2023,
			title     = {Unlocking the Heterogeneous Landscape of Big Data {NLP} with {DUUI}},
			author    = {Leonhardt, Alexander and Abrami, Giuseppe and Baumartz, Daniel and Mehler, Alexander},
			editor    = {Bouamor, Houda and Pino, Juan and Bali, Kalika},
			booktitle = {Findings of the Association for Computational Linguistics: EMNLP 2023},
			year      = {2023},
			address   = {Singapore},
			publisher = {Association for Computational Linguistics},
			url       = {https://aclanthology.org/2023.findings-emnlp.29},
			pages     = {385--399},
			pdf       = {https://aclanthology.org/2023.findings-emnlp.29.pdf},
			abstract  = {Automatic analysis of large corpora is a complex task, especially
			in terms of time efficiency. This complexity is increased by the
			fact that flexible, extensible text analysis requires the continuous
			integration of ever new tools. Since there are no adequate frameworks
			for these purposes in the field of NLP, and especially in the
			context of UIMA, that are not outdated or unusable for security
			reasons, we present a new approach to address the latter task:
			Docker Unified UIMA Interface (DUUI), a scalable, flexible, lightweight,
			and feature-rich framework for automatic distributed analysis
			of text corpora that leverages Big Data experience and virtualization
			with Docker. We evaluate DUUI{'}s communication approach against
			a state-of-the-art approach and demonstrate its outstanding behavior
			in terms of time efficiency, enabling the analysis of big text
			data.}
			}
	`

</script>

<div class="section-wrapper !bg-surface-100-800-token bordered-soft p-4 w-full md:w-modal-wide">
	<div class="modal-header">
		<h1 class="h3">How to Cite</h1>
		<button on:click={() => modalStore.close()} class="transition-300 hover:text-error-500">
			<Fa icon={faClose} size="lg" />
		</button>
	</div>
	<div>
		<p>For academic citations, please use:</p>
		<pre><code>
			{citation}
	  	</code></pre>
		<button
			class="button-neutral "
			use:clipboard={citation}
			on:click={() => {
				toastStore.trigger(successToast('Copied!'))
			}}
			>
				<Fa icon={faClipboard} />
				Copy
		</button>
	</div>
</div>
