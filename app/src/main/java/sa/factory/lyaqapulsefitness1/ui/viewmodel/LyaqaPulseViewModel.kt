package sa.factory.lyaqapulsefitness1.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.lyaqapulsefitness1.domain.usecase.*
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseItem
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseOrder
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LyaqaPulseUiState(
    val catalog: List<LyaqaPulseItem> = emptyList(),
    val filteredCatalog: List<LyaqaPulseItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<LyaqaPulseItem> = emptyList(),
    val telemetry: LyaqaPulseTelemetry? = null,
    val lastOrder: LyaqaPulseOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class LyaqaPulseViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyaqaPulseUiState())
    val uiState: StateFlow<LyaqaPulseUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: LyaqaPulseItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: LyaqaPulseItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
