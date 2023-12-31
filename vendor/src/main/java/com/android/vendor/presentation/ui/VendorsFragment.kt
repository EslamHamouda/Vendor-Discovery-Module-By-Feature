package com.android.vendor.presentation.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearSnapHelper
import com.android.core.utils.isShowProgressBar
import com.android.core.utils.showSnackBar
import com.android.vendor.databinding.FragmentVendorsBinding
import com.android.vendor.presentation.adapter.VendorCategoryAdapter
import com.android.vendor.presentation.adapter.VendorsPagerAdapter
import com.android.vendor.presentation.adapter.VendorsPagerLoadStateAdapter
import com.android.vendor.presentation.viewmodel.VendorCategoryStates
import com.android.vendor.presentation.viewmodel.VendorsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VendorsFragment : Fragment() {
    private var _binding: FragmentVendorsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VendorsViewModel by viewModels()
    private var categoryIdText: String? = null
    private val vendorsPagerAdapter by lazy {
        VendorsPagerAdapter(object : VendorsPagerAdapter.OnItemClickListener {
            override fun onVendorClick(vendorId: String) {
                NavDeepLinkRequest.Builder.fromUri(
                    Uri.parse(
                        getString(com.android.core.R.string.vendor_merchants_link).replace(
                            "{vendorId}",
                            vendorId
                        )
                    )
                ).build().apply { findNavController().navigate(this) }
            }
        })
    }
    private val vendorCategoryAdapter by lazy {
        VendorCategoryAdapter(object : VendorCategoryAdapter.OnItemClickListener {
            override fun onCategoryClicked(categoryId: String) {
                viewModel.getVendors(categoryId)
                collectVendors()
                categoryIdText = categoryId
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentVendorsBinding.inflate(inflater)
        viewModel.getVendorCategory()
        viewModel.getVendors()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        collectVendorCategory()
        collectVendors()
        binding.searchCategories.setOnQueryTextListener(object : OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.getVendors(categoryIdText, newText)
                collectVendors()
                return true
            }
        })
    }

    private fun setupUI() {
        binding.rvVendorCategories.apply {
            adapter = vendorCategoryAdapter
            LinearSnapHelper().attachToRecyclerView(this)
        }
        binding.rvCategoryContent.adapter =
            vendorsPagerAdapter.withLoadStateHeaderAndFooter(
                header = VendorsPagerLoadStateAdapter(vendorsPagerAdapter::retry),
                footer = VendorsPagerLoadStateAdapter(vendorsPagerAdapter::retry)
            )
    }

    private fun collectVendorCategory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getVendorCategoryResponse.collectLatest {
                    when (it) {
                        is VendorCategoryStates.Loading -> {
                            binding.progressBar.progressBar.isShowProgressBar(it.isLoading)
                        }

                        is VendorCategoryStates.Success -> {
                            vendorCategoryAdapter.submitList(it.value)
                        }

                        is VendorCategoryStates.Failure -> {
                            it.throwable.message?.let { it1 ->
                                showSnackBar(
                                    it1,
                                    requireActivity()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun collectVendors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getVendorsResponse.collectLatest {
                    vendorsPagerAdapter.submitData(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}