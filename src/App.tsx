/**
 * Trading Challenge Tracker - متتبع تحدي التداول
 * Personal Trading Challenge Management Application with 150 Milestones.
 */

import React, { useState, useEffect, useCallback } from 'react';
import { ActiveTab, Challenge, Milestone, Trade } from './types';
import { StorageService } from './services/storage';
import { formatMoney, parseCapitalToCents } from './utils/money';
import { Navbar } from './components/Navbar';
import { DashboardScreen } from './components/DashboardScreen';
import { TradeHistoryScreen } from './components/TradeHistoryScreen';
import { RoadmapScreen } from './components/RoadmapScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { AddTradeModal } from './components/AddTradeModal';
import { TradeDetailModal } from './components/TradeDetailModal';
import { MilestoneDetailModal } from './components/MilestoneDetailModal';
import { PdfReportModal } from './components/PdfReportModal';
import { ImageViewerModal } from './components/ImageViewerModal';
import { ConfirmModal } from './components/ConfirmModal';
import { Smartphone, Monitor, Wifi, Battery, Signal } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('dashboard');
  const [challenge, setChallenge] = useState<Challenge>(StorageService.getChallenge());
  const [trades, setTrades] = useState<Trade[]>(StorageService.getTrades());
  const [milestones, setMilestones] = useState<Milestone[]>(StorageService.getMilestones());

  // UI Modals
  const [isAddTradeOpen, setIsAddTradeOpen] = useState(false);
  const [isPdfReportOpen, setIsPdfReportOpen] = useState(false);
  const [selectedTrade, setSelectedTrade] = useState<Trade | null>(null);
  const [selectedMilestone, setSelectedMilestone] = useState<Milestone | null>(null);
  const [viewingImageUrl, setViewingImageUrl] = useState<string | null>(null);
  const [notificationMessage, setNotificationMessage] = useState<string | null>(null);

  // Mobile Frame Toggle
  const [isMobileFrame, setIsMobileFrame] = useState(true);

  // Confirmation Modals State
  const [confirmDialog, setConfirmDialog] = useState<{
    isOpen: boolean;
    type: 'start' | 'reset' | 'new_challenge';
    title: string;
    message: string;
    highlightText?: string;
    isDestructive?: boolean;
    onConfirm: () => void;
  }>({
    isOpen: false,
    type: 'start',
    title: '',
    message: '',
    onConfirm: () => {},
  });

  // Prompt dialog for new challenge capital input
  const [newCapitalModal, setNewCapitalModal] = useState<{
    isOpen: boolean;
    capitalStr: string;
    error: string | null;
  }>({
    isOpen: false,
    capitalStr: '500',
    error: null,
  });

  // Reload state from storage
  const refreshData = useCallback(() => {
    const loadedChallenge = StorageService.getChallenge();
    const loadedTrades = StorageService.getTrades();
    const loadedMilestones = StorageService.getMilestones(loadedChallenge.id);

    setChallenge(loadedChallenge);
    setTrades(loadedTrades);
    setMilestones(loadedMilestones);
  }, []);

  useEffect(() => {
    refreshData();
  }, [refreshData]);

  // Show temporary feedback banner
  const triggerNotification = (msg: string) => {
    setNotificationMessage(msg);
    setTimeout(() => {
      setNotificationMessage(null);
    }, 4000);
  };

  // Handle Trade Submission
  const handleTradeSubmit = async (
    resultCents: number,
    attachmentBase64: string | null
  ) => {
    const result = StorageService.recordTrade(resultCents, attachmentBase64);
    refreshData();
    triggerNotification(`تم تسجيل الصفقة #${result.trade.tradeNumber} بنجاح`);
  };

  // Handle Settings Save
  const handleSaveSettings = (
    userName: string,
    initialCapitalCents: number,
    targetBalanceCents: number
  ) => {
    StorageService.updateSettings(userName, initialCapitalCents, targetBalanceCents);
    refreshData();
  };

  // Handle Start Challenge
  const handleStartChallengeClick = () => {
    setConfirmDialog({
      isOpen: true,
      type: 'start',
      title: 'تأكيد بدء التحدي',
      message: 'هل أنت متأكد من بدء التحدي الآن؟',
      highlightText: `سيتم بدء التحدي برأس مال ${formatMoney(challenge.initialCapitalCents)}`,
      isDestructive: false,
      onConfirm: () => {
        StorageService.startChallenge();
        refreshData();
        setConfirmDialog((prev) => ({ ...prev, isOpen: false }));
        triggerNotification(`تم بدء التحدي برأس مال ${formatMoney(challenge.initialCapitalCents)}`);
      },
    });
  };

  // Handle Reset Challenge
  const handleResetChallengeClick = () => {
    setConfirmDialog({
      isOpen: true,
      type: 'reset',
      title: 'إعادة التحدي؟',
      message: 'سيؤدي هذا الإجراء إلى حذف سجل الصفقات الحالي وإعادة جميع المحطات إلى حالتها الأولية.\nهل أنت متأكد؟',
      isDestructive: true,
      onConfirm: () => {
        StorageService.resetChallenge();
        refreshData();
        setConfirmDialog((prev) => ({ ...prev, isOpen: false }));
        triggerNotification('تم إعادة ضبط التحدي بنجاح');
      },
    });
  };

  // Handle Start New Challenge Click
  const handleNewChallengeClick = () => {
    setNewCapitalModal({
      isOpen: true,
      capitalStr: (challenge.initialCapitalCents / 100).toString(),
      error: null,
    });
  };

  const handleConfirmNewCapital = () => {
    const parsed = parseCapitalToCents(newCapitalModal.capitalStr);
    if (!parsed.valid || parsed.cents === undefined) {
      setNewCapitalModal((prev) => ({
        ...prev,
        error: parsed.error || 'يرجى إدخال رأس مال صحيح أكبر من صفر.',
      }));
      return;
    }

    const newCapitalCents = parsed.cents;
    setNewCapitalModal((prev) => ({ ...prev, isOpen: false }));

    // Show warning confirmation
    setConfirmDialog({
      isOpen: true,
      type: 'new_challenge',
      title: 'بدء تحدٍ جديد؟',
      message: `سيتم بدء تحدٍ جديد كلياً برأس مال ${formatMoney(newCapitalCents)}.\nسيتم حذف سجل الصفقات الحالي وإعادة تعيين الـ 150 محطة.\nهل أنت متأكد من المتابعة؟`,
      highlightText: formatMoney(newCapitalCents),
      isDestructive: true,
      onConfirm: () => {
        StorageService.startNewChallenge(newCapitalCents);
        refreshData();
        setConfirmDialog((prev) => ({ ...prev, isOpen: false }));
        triggerNotification(`تم بدء التحدي الجديد بنجاح برأس مال ${formatMoney(newCapitalCents)}`);
      },
    });
  };

  return (
    <div className="min-h-screen bg-[#070a10] text-slate-100 flex flex-col items-center justify-start sm:py-6 selection:bg-amber-400 selection:text-black">
      {/* Top Device Viewport Controls */}
      <header className="w-full max-w-md px-4 py-2 flex items-center justify-between text-xs text-slate-400 border-b border-slate-900/60 mb-1 sm:mb-3">
        <span className="font-bold text-slate-300 tracking-wide flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>Trading Challenge Tracker</span>
        </span>

        <button
          onClick={() => setIsMobileFrame(!isMobileFrame)}
          className="flex items-center gap-1.5 py-1 px-2.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-amber-400 border border-slate-800 transition-colors"
          title="تبديل حجم الإطار"
        >
          {isMobileFrame ? (
            <>
              <Monitor className="w-3.5 h-3.5" />
              <span>شاشة كاملة</span>
            </>
          ) : (
            <>
              <Smartphone className="w-3.5 h-3.5" />
              <span>إطار الهاتف</span>
            </>
          )}
        </button>
      </header>

      {/* Main Container / Mobile Device Wrapper */}
      <main
        className={`w-full relative flex flex-col bg-[#0b0f17] transition-all duration-300 ${
          isMobileFrame
            ? 'max-w-md min-h-[92vh] sm:min-h-[844px] sm:max-h-[94vh] sm:rounded-[40px] sm:border-[6px] sm:border-slate-800/90 sm:shadow-[0_25px_60px_-15px_rgba(0,0,0,0.9)] overflow-hidden'
            : 'max-w-2xl min-h-screen'
        }`}
      >
        {/* Mobile Device Status Bar matching screenshot (16:20, Signal, Wifi, Battery) */}
        <div className="flex items-center justify-between px-6 pt-3 pb-1 text-xs text-slate-400 select-none z-30 shrink-0">
          <span className="font-['JetBrains_Mono',monospace] font-bold text-slate-200">
            16:20
          </span>

          {/* Device Speaker Notch in Phone Frame */}
          <div className="hidden sm:block w-24 h-4 bg-slate-900/90 rounded-full border border-slate-800/80 shadow-inner" />

          <div className="flex items-center gap-1.5">
            <Signal className="w-3.5 h-3.5 text-slate-300" />
            <Wifi className="w-3.5 h-3.5 text-slate-300" />
            <Battery className="w-4 h-4 text-slate-200" />
          </div>
        </div>

        {/* Inner Scrollable Screen Content */}
        <div className="flex-1 overflow-y-auto px-4 pt-2">
          {activeTab === 'dashboard' && (
            <DashboardScreen
              challenge={challenge}
              notificationMessage={notificationMessage}
              onOpenSettings={() => setActiveTab('settings')}
              onOpenAddTrade={() => setIsAddTradeOpen(true)}
              onStartChallengeClick={handleStartChallengeClick}
              onNewChallengeClick={handleNewChallengeClick}
            />
          )}

          {activeTab === 'history' && (
            <TradeHistoryScreen
              trades={trades}
              onOpenPdfReport={() => setIsPdfReportOpen(true)}
              onSelectTrade={(t) => setSelectedTrade(t)}
              onOpenAddTrade={() => setIsAddTradeOpen(true)}
            />
          )}

          {activeTab === 'roadmap' && (
            <RoadmapScreen
              milestones={milestones}
              onSelectMilestone={(m) => setSelectedMilestone(m)}
            />
          )}

          {activeTab === 'settings' && (
            <SettingsScreen
              challenge={challenge}
              onSaveSettings={handleSaveSettings}
              onResetChallengeClick={handleResetChallengeClick}
              onNewChallengeClick={handleNewChallengeClick}
            />
          )}
        </div>

        {/* Bottom Navigation */}
        <Navbar activeTab={activeTab} onTabChange={(tab) => setActiveTab(tab)} />
      </main>

      {/* Add Trade Modal */}
      <AddTradeModal
        isOpen={isAddTradeOpen}
        currentBalanceCents={challenge.currentBalanceCents}
        tradeCount={challenge.tradeCount}
        onClose={() => setIsAddTradeOpen(false)}
        onSubmit={handleTradeSubmit}
      />

      {/* Trade Detail Modal */}
      <TradeDetailModal
        trade={selectedTrade}
        onClose={() => setSelectedTrade(null)}
        onViewImage={(url) => setViewingImageUrl(url)}
      />

      {/* Milestone Detail Modal */}
      <MilestoneDetailModal
        milestone={selectedMilestone}
        onClose={() => setSelectedMilestone(null)}
        onViewImage={(url) => setViewingImageUrl(url)}
      />

      {/* PDF Printable Report Modal */}
      <PdfReportModal
        isOpen={isPdfReportOpen}
        challenge={challenge}
        trades={trades}
        onClose={() => setIsPdfReportOpen(false)}
      />

      {/* Fullscreen Image Lightbox */}
      <ImageViewerModal
        imageUrl={viewingImageUrl}
        onClose={() => setViewingImageUrl(null)}
      />

      {/* Confirmation Dialog */}
      <ConfirmModal
        isOpen={confirmDialog.isOpen}
        type={confirmDialog.type}
        title={confirmDialog.title}
        message={confirmDialog.message}
        highlightText={confirmDialog.highlightText}
        isDestructive={confirmDialog.isDestructive}
        onConfirm={confirmDialog.onConfirm}
        onCancel={() => setConfirmDialog((prev) => ({ ...prev, isOpen: false }))}
      />

      {/* New Challenge Capital Input Modal */}
      {newCapitalModal.isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="bg-[#121824] border border-slate-800 rounded-2xl p-6 max-w-sm w-full shadow-2xl text-right">
            <h3 className="text-base font-bold text-slate-100 mb-2">
              بدء تحدٍ جديد - تحديد رأس المال
            </h3>
            <p className="text-xs text-slate-400 mb-4 leading-relaxed">
              أدخل رأس المال الابتدائي الذي تريد أن تبدأ به التحدي الجديد:
            </p>

            <div className="mb-4">
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                رأس المال الابتدائي ($)
              </label>
              <input
                type="text"
                inputMode="decimal"
                value={newCapitalModal.capitalStr}
                onChange={(e) =>
                  setNewCapitalModal((prev) => ({
                    ...prev,
                    capitalStr: e.target.value,
                    error: null,
                  }))
                }
                placeholder="500"
                className="w-full bg-[#141b2a] border border-slate-700/80 rounded-xl px-4 py-3 text-slate-100 text-sm font-['JetBrains_Mono',monospace] focus:outline-none focus:border-amber-400"
                dir="ltr"
              />

              {/* Quick chips */}
              <div className="flex flex-wrap gap-1.5 mt-2.5">
                {[50, 100, 250, 500, 1000, 5000].map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() =>
                      setNewCapitalModal((prev) => ({
                        ...prev,
                        capitalStr: val.toString(),
                        error: null,
                      }))
                    }
                    className="py-1 px-2.5 rounded-lg text-xs font-['JetBrains_Mono',monospace] font-bold bg-slate-900 border border-slate-800 hover:border-amber-400 text-slate-300 hover:text-amber-400 transition-colors"
                  >
                    ${val}
                  </button>
                ))}
              </div>
            </div>

            {newCapitalModal.error && (
              <p className="text-xs text-rose-400 mb-4">{newCapitalModal.error}</p>
            )}

            <div className="flex items-center gap-2 pt-2">
              <button
                onClick={handleConfirmNewCapital}
                className="flex-1 py-3 rounded-xl bg-amber-400 hover:bg-amber-300 font-bold text-slate-950 text-xs transition-colors shadow-md"
              >
                متابعة
              </button>
              <button
                onClick={() => setNewCapitalModal((prev) => ({ ...prev, isOpen: false }))}
                className="flex-1 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 font-bold text-slate-300 text-xs transition-colors"
              >
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
